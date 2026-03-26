from fastapi import APIRouter

from services.release_agent import get_release_agent

router = APIRouter(prefix="/announcement", tags=["AI测试"])


@router.get("/test-mode")
async def test_llm(prompt: str = "你好，请自我介绍"):
    """
    测试通义千问模型是否打通 (流式输出)
    """
    model = get_release_agent()
    from langchain_core.messages import HumanMessage
    response = model.invoke([HumanMessage(content=prompt)])
    return {"status": "success", "content": response.content}
