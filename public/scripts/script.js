"use strict"

import {marked} from "../libs/marked.esm.min.js"

const conversationElement = document.getElementById('conversation');
const chatHistoryElement = document.getElementById('chat-history');
const chatsElement = document.getElementById('chats');
const sendChatButton = document.getElementById('send-chat');
const newConversationButton = document.getElementById('new-conversation');
const textArea = document.querySelector('textarea');
const thinkArticle = document.querySelector(".thinking");

let chatList;
let currentConversation;
let currentConversationId = 1;

sendChatButton.addEventListener('click', () => {
    doRequestToBackend(textArea.value)
})

newConversationButton.addEventListener('click', createNewConversation);

delegateEvent('#chats', '.select-btn', 'click', (event) => {
    const chatId = event.target.dataset.chatId;
    currentConversationId = chatId;
    currentConversation = null;
    chooseChat(chatId);
})

delegateEvent('#chats', '.delete-btn', 'click', (event) => {
    const button = event.target.closest('.delete-btn'); // Hitta närmaste .delete-btn
    if (button) {
        const chatId = button.dataset.chatId; // Hämta data-attributet från knappen
        console.log(chatId);
        fetch("/api/chat/delete-chat?id=" + chatId, {
            method: "DELETE",
            headers: {
                "Content-Type": "application/json"
            }
        })
            .then(response => response.json())
            .then(jsonResponse => {
                console.log(jsonResponse)
                const element = document.querySelector(`li[data-chat-id="${chatId}"]`);
                if (element) {
                    element.remove(); // Ta bort elementet från DOM
                }
            })
    }
});

fetch("/api").then(res => res.text()).then(text => console.log(text))

getAllChats();

function getAllChats() {
    fetch("/api/chat/get-all").then(res => res.json()).then(jsonRes => {
        chatList = jsonRes.chatList.reverse();

        populateChats(chatList)

        const latestChat = chatList.at(0);

        currentConversation = latestChat.conversation;
        currentConversationId = latestChat.id;

        populateConversation(currentConversation);
    });
}

function doRequestToBackend(question) {
    const requestData = {
        query: question,
        id: currentConversationId // För nuvarande sätter vi ID till 1
    };

    const newestquery = {
        type: "UserMessage",
        text: question
    }
    currentConversation.messages.push(newestquery);
    populateConversation(currentConversation);

    fetch("/api/chat", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(requestData)
    })
        .then(response => {
            const reader = response.body.getReader();
            const decoder = new TextDecoder();
            let partialResponse = "";
            let partialThinking = "";
            let isThinking = false;
            const details = document.createElement('details');
            details.open = true;
            const summary = document.createElement('summary');
            summary.innerText = "Klicka här för att se resonemanget";


            return new ReadableStream({
                start(controller) {
                    function push() {
                        reader.read().then(({done, value}) => {
                            if (done) {
                                controller.close();
                                return;
                            }

                            const textChunk = decoder.decode(value, {stream: true});
                            console.log("Mottagen chunk:", textChunk);

                            if (textChunk === "<think>") {
                                isThinking = true;
                                thinkArticle.prepend(details)
                            }

                            if (textChunk === "</think>")
                                isThinking = false;


                            if (isThinking) {
                                if (textChunk !== "<think>") {
                                    partialThinking += textChunk;
                                    console.log(partialThinking)
                                    details.innerHTML = marked(partialThinking);
                                    details.prepend(summary);
                                }
                            } else if (textChunk !== "</think>") {
                                partialResponse += textChunk;
                                console.log(partialResponse)
                                conversationElement.innerHTML = marked(partialResponse);
                            }

                            controller.enqueue(value);
                            push();
                        });
                    }

                    push();
                }
            });
        })
        .then(stream => new Response(stream))
        .then(response => response.text())
        .then(text => {
            console.log("Hela svaret:", text)
            const newestAnswer = {
                type: "AIMessage",
                text: text
            };
            currentConversation.messages.push(newestAnswer);
            populateConversation(currentConversation);
            conversationElement.replaceChildren();
            thinkArticle.replaceChildren();
        })
        .catch(error => console.error("Fel vid hämtning:", error));

}

function createNewConversation() {
    fetch("/api/chat/new-conversation", {
        method: "POST"
    })
        .then(response => response.json())
        .then(res => {
            currentConversationId = res.id;
            chatHistoryElement.replaceChildren();
            currentConversation.messages = [];
            getAllChats();
        })
}

function chooseChat(id) {
    // Hämta alla knappar och ta bort "active" klassen
    document.querySelectorAll("#chats button").forEach(button => {
        button.classList.remove("active");
    });

    // Hitta den valda knappen och lägg till "active" klassen
    const activeButton = document.querySelector(`#chats button[data-chat-id="${id}"]`);
    if (activeButton) {
        activeButton.classList.add("active");
    }

    fetch("/api/chat/get-chat?id=" + id, {
        method: "GET",
        headers: {
            "Content-Type": "application/json"
        }
    })
        .then(res => res.json())
        .then(json => {
            currentConversation = json.conversation;
            populateConversation(json.conversation);
        })
}

function populateChats(chats) {
    const fragment = document.createDocumentFragment();

    chats.forEach((chat, index)=> {
        const li = document.createElement('li');
        const btn = document.createElement('button');
        const deleteBtn = document.createElement('button');
        const deleteImg = document.createElement('img');
        deleteImg.src = "icons/delete_forever.svg";
        deleteBtn.setAttribute('data-chat-id', chat.id)
        deleteBtn.classList.add('delete-btn')
        deleteBtn.appendChild(deleteImg)

        btn.innerText = chat.label ? chat.label : "New chat";
        btn.setAttribute('data-chat-id', chat.id);
        btn.classList.add('select-btn')

        // Lägg till "active" på den första knappen
        if (index === 0) {
            btn.classList.add("active");
        }

        li.setAttribute('data-chat-id', chat.id);
        li.appendChild(btn);
        li.appendChild(deleteBtn)
        fragment.appendChild(li);
    });

    chatsElement.replaceChildren(fragment);
}

function populateConversation(newConversationToReplaceOld) {
    const fragment = document.createDocumentFragment();

    newConversationToReplaceOld.messages.forEach(message => {
        // Skapa ett nytt fragment
        const tempFragment = document.createDocumentFragment();
        // Skapa ett temporärt element för att konvertera HTML-strängen till DOM-noder
        const tempElement = document.createElement("template");
        tempElement.innerHTML = marked(message.text);
        // Lägg till innehållet i tempElement i det temporära fragmentet
        tempFragment.appendChild(tempElement.content);
        // Lägg till det temporära fragmentet i huvudfragmentet
        fragment.appendChild(tempFragment);
    });

    chatHistoryElement.replaceChildren(fragment);
}

function delegateEvent(parentSelector, childSelector, eventType, callback) {
    document.querySelector(parentSelector).addEventListener(eventType, function(event) {
        const targetElement = event.target.closest(childSelector);
        if (targetElement && this.contains(targetElement)) {
            callback(event);
        }
    });
}