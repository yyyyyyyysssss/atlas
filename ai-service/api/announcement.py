import json
from datetime import datetime

from fastapi import APIRouter, Query
from langchain_core.messages import HumanMessage
from starlette.responses import StreamingResponse

from services.release_agent import create_release_agent

router = APIRouter(prefix="/ai/agent", tags=["AI发版公告"])


@router.get("/release")
async def release_assistant(
        days: int = Query(7, ge=1, le=30, description="回顾的代码提交天数"),
        version: str = Query(None, description="可选：指定目标版本号")
):
    """
    🚀 触发 AI Agent 生成发版公告
    - 采用 SSE 流式协议回传节点状态
    - 自动触发 Git 日志审计与 DTO 封装
    """
    agent_app = create_release_agent()

    async def event_generator():
        # 1. 构造初始指令
        prompt = f"生成最近 {days} 天的代码发版说明。"
        if version:
            prompt += f" 目标版本定为 {version}。"

        inputs = {"messages": [HumanMessage(content=prompt)]}
        try:
            # 2. 异步流式运行 LangGraph 节点
            # stream_mode="updates" 能让我们捕获每个节点的 return 结果
            async for event in agent_app.astream(inputs, stream_mode="updates"):
                for node_name, output in event.items():
                    # 构造推送给前端的消息包
                    payload = {
                        "node": node_name,
                        "status": "completed",
                        "time": datetime.now().strftime("%H:%M:%S"),
                        "payload": {}
                    }
                    # 针对不同节点提取关键数据
                    if node_name == "agent":
                        msg = output["messages"][-1]
                        # 如果 AI 输出了 Markdown 内容，传给前端展示
                        payload["payload"] = {"content": msg.content}
                    elif node_name == "tools":
                        payload["payload"] = {"msg": "工具调用已完成"}
                    elif node_name == "save_db":
                        # 拿到最终的任务报告
                        payload["payload"] = {"msg": "公告已成功同步至 Atlas 后端"}

                    yield f"data: {json.dumps(payload, ensure_ascii=False)}\n\n"
                    # 3. 结束标志
            yield "data: {\"status\": \"end\", \"msg\": \"Agent 任务全部闭环\"}\n\n"

        except Exception as e:
            error_payload = {"status": "error", "msg": str(e)}
            yield f"data: {json.dumps(error_payload)}\n\n"

    return StreamingResponse(event_generator(), media_type="text/event-stream")
