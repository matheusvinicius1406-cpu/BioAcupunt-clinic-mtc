from sentence_transformers import SentenceTransformer

# Carrega o modelo de embeddings (all-MiniLM-L6-v2)
model = SentenceTransformer('all-MiniLM-L6-v2')

def get_embedding(text: str):
    return model.encode(text).tolist()
