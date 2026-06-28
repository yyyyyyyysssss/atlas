import os

import uvicorn
from dotenv import load_dotenv, find_dotenv
from fastapi import FastAPI
from starlette import status

# 加载环境
load_dotenv(find_dotenv())

app_port = int(os.getenv("APP_PORT", 8000))
app_host = os.getenv("APP_HOST", "127.0.0.1")

app = FastAPI(
    title='ai-service',
    description='AI Service',
    version='1.0.0'
)


@app.get("/")
def read_root():
    return {"Hello": "Atlas AI Service"}


@app.get("/health", status_code=status.HTTP_200_OK, tags=["System"])
def health_check():
    # 正常情况
    return {
        "status": "UP",
        "version": "1.0.0",
    }


if __name__ == '__main__':
    # 启动
    uvicorn.run(
        'main:app',
        host=app_host,
        port=app_port,
        reload=True
    )
