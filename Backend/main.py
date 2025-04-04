from fastapi import FastAPI
from pydantic import BaseModel
from openai import OpenAI

# Initialize FastAPI
app = FastAPI()

# Replace with your LangDB API details
LANGDB_PROJECT_ID = "f4387c82-9880-4fae-9164-6a5150a0759c"  # LangDB Project ID
API_KEY = "langdb_NW5nYXAyOW81bnQ4bzlsdWVqamhhMzdpY2Y="  # LangDB Token

client = OpenAI(
    base_url=f"https://api.us-east-1.langdb.ai/{LANGDB_PROJECT_ID}/v1",
    api_key=API_KEY
)

# Define request body structure
class ChatRequest(BaseModel):
    message: str

@app.post("/chat")
def chat_with_ai(request: ChatRequest):
    """Handles user input and returns AI response."""
    response = client.chat.completions.create(
        model="gpt-4o",
        messages=[
            {"role": "system", "content": "You are a helpful assistant"},
            {"role": "user", "content": request.message}
        ],
    )
    return {"response": response.choices[0].message.content}.get('response')
