import os

import uvicorn
from fastapi import FastAPI
from dotenv import load_dotenv, find_dotenv
from sqlalchemy.sql.operators import truediv

from api.announcement import router as ai_announcement_router

app = FastAPI(
    title='ai-service',
    description='本服务由 LangChain 驱动，对接通义千问大模型',
    version='1.0.0'
)

app.include_router(ai_announcement_router)

@app.get("/")
def read_root():
    return {"Hello": "Atlas AI User"}

if __name__ == '__main__':
    # 加载环境
    load_dotenv(find_dotenv())
    # 启动
    uvicorn.run(
        'main:app',
        host= 'localhost',
        port = 9030,
        reload = True
    )
