import { marked } from "../libs/marked.esm.min.js"

const article = document.getElementById('conversation');
const chatsElement = document.getElementById('chats');
const button = document.querySelector('button');
const textArea = document.querySelector('textarea');

let chatList;

button.addEventListener('click', () => {
    doRequestToBackend(textArea.value)
})

fetch("/api").then(res => res.text()).then(text => console.log(text))
fetch("/api/chat/get-all").then(res => res.json()).then(jsonRes => {
    console.log(jsonRes.chatList)
    chatList = jsonRes.chatList;

    populateChats(chatList)

    const conversation = chatList.find(chat => chat.id === 1).conversation;
    populateConversation(conversation);
});

function doRequestToBackend(question) {
    const requestData = {
        query: question,
        id: 1 // För nuvarande sätter vi ID till 1
    };

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
            const thinkArticle = document.querySelector(".thinking");
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
                                article.innerHTML = marked(partialResponse);
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
        .then(text => console.log("Hela svaret:", text))
        .catch(error => console.error("Fel vid hämtning:", error));

}

function populateChats(chats) {
    const fragment = document.createDocumentFragment();

    chats.forEach(chat => {
        const li = document.createElement('li');
        li.innerText = chat.label;
        li.setAttribute('data-chat-id', chat.id)
        fragment.appendChild(li);
    });

    chatsElement.replaceChildren(fragment);
}

function populateConversation(conversation) {
    conversation.messages.forEach(message => {
        console.log(message.type)
        console.log(message.text)
    })
}