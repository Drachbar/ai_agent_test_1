import { marked } from "../libs/marked.esm.min.js"

const pTag = document.querySelector('section');
const button = document.querySelector('button');
const textArea = document.querySelector('textarea');

button.addEventListener('click', () => {
    doRequestToBackend(textArea.value)
})

function doRequestToBackend(question) {

    fetch("/api/chat", {
        method: "POST",
        body: question
    })
        .then(response => {
            const reader = response.body.getReader();
            const decoder = new TextDecoder();
            let partialResponse = "";

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

                            // Lägg till den mottagna texten i pTag
                            partialResponse += textChunk;
                            console.log(partialResponse)

                            pTag.innerHTML = marked(partialResponse);

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