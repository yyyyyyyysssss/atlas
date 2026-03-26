import datetime
import operator
import os
import subprocess
from datetime import datetime, timedelta
from typing import Any, TypedDict, Annotated, List

import httpx
from dotenv import load_dotenv
from langchain_community.chat_models import ChatTongyi
from langchain_core.callbacks import StreamingStdOutCallbackHandler
from langchain_core.messages import BaseMessage, HumanMessage, ToolMessage, SystemMessage, AIMessage
from langchain_core.tools import tool
from pydantic import BaseModel, Field, field_validator
from langgraph.graph import StateGraph, END


# 定义颜色常量 (ANSI Escape Codes)
GREEN = "\033[92m"
BLUE = "\033[94m"
CYAN = "\033[96m"
YELLOW = "\033[93m"
RED = "\033[91m"
BOLD = "\033[1m"
RESET = "\033[0m"
PURPLE = "\033[95m"

class AnnouncementDTO(BaseModel):
    """发版公告的结构化数据模型，对接后端 Java 接口"""
    title: str = Field(description="【必填】公告标题，例如: Atlas v2.7.0 发版说明")
    description: str = Field(description="【必填】列表摘要，简述本次更新核心点")
    content: str = Field(description="完整的标准 Markdown 正文内容，包含所有章节和分割线")
    version: str = Field(description="【必填】版本号，格式如 v2.7.0")


# ==========================================
# 工具定义 (保留你之前的逻辑)
# ==========================================

@tool
def get_current_version() -> str:
    """
        获取 Atlas 系统的当前最新线上版本号。
        在计算新版本号或撰写发版公告前，必须调用此工具以获取基准版本。
    """
    url = "http://localhost:9099/announcement/latest/version"
    print(f"\n🌐 [VersionTool] 正在请求 Java 接口: {url}")
    # 调用失败或返回null时返回默认版本让 Agent 逻辑继续
    try:
        with httpx.Client(timeout=3.0) as client:
            result = client.get(url)
            print(f"📡 [VersionTool] HTTP 状态码: {result.status_code}")
            if result.status_code == 200:
                res_data = result.json()
                print(f"DEBUG: result = {res_data}")
                if res_data.get('code') == 0:
                    version = res_data.get("data")
                    print(f"✅ [VersionTool] 解析到版本号: {version if version else 'None (将使用默认值)'}")
                    return version or "v1.0.0"
            return "v1.0.0"
    except Exception as ex:
        print(f"❌ [VersionTool] 网络异常: {str(ex)}")
        return "v1.0.0"


# 定义获取git日志工具的参数模型 用来限制模型调用该工具时所输入的参数
class GitLogInput(BaseModel):
    days: int = Field(
        default=7,
        description='需要获取多少天以内的代码提交记录。例如输入 3 表示获取最近 3 天的日志。'
    )

    # 核心：增加校验器来拦截并清洗数据
    @field_validator('days', mode='before')
    @classmethod
    def decode_days_string(cls, v: Any) -> int:
        # 如果模型抽风传了 "days=7"
        print(f"DEBUG: Pydantic 接收到的原始 days 值 = {v}")
        if isinstance(v, str):
            # 提取字符串中的所有数字
            digits = ''.join(c for c in v if c.isdigit())
            if digits:
                return int(digits)
        # 如果是正常的数字或无法解析，返回原值让 Pydantic 继续后续校验
        return v


# 将 schema 绑定到工具上
@tool(args_schema=GitLogInput)
def get_git_logs(days: int = 7):
    """
        检索当前 Atlas 仓库的 Git 提交日志。
        1、当用户提到“最近更新”、“代码变动”、“这周做了什么”时触发。
        2. 当需要生成发版公告、周报、技术总结的素材时触发。
        注意：返回的是原始提交记录，需要结合 LLM 进一步整理。
    """
    days_int = int(days)
    # 计算起始日期
    since_date = (datetime.now() - timedelta(days=days_int)).strftime('%Y-%m-%d')
    print(f"\n🔍 [GitTool] 正在尝试获取自 {since_date} 以来（最近 {days_int} 天）的日志...")
    try:
        # git log 命令：格式化为 "日期 | 作者 | 消息"
        cmd = [
            'git', 'log',
            f'--since={since_date}',
            '--no-merges',  # 过滤掉合并分支的记录，只保留真实的业务提交
            '--pretty=format:%ad | %an | %s',
            '--date=short'
        ]
        current_dir = os.path.dirname(os.path.abspath(__file__))
        print(f"📂 [GitTool] 当前执行路径: {current_dir}")
        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            encoding='utf-8',
            errors='ignore',
            cwd=current_dir
        )
        if result.returncode != 0:
            error_msg = f"Git 命令执行失败，错误输出: {result.stderr}"
            print(f"❌ [GitTool] {error_msg}")
            return error_msg
        logs = result.stdout.strip()
        if not logs:
            msg = f"自 {since_date} 以来无提交记录。"
            print(f"⚠️ [GitTool] {msg}")
            return msg

        log_count = len(logs.split('\n'))
        print(f"✅ [GitTool] 成功获取到 {log_count} 条提交记录。")
        return logs

    except Exception as ex:
        error_msg = f"执行 Git 命令时发生异常: {str(ex)}"
        print(f"❌ [GitTool] {error_msg}")
        return error_msg


@tool(args_schema=AnnouncementDTO)
def submit_announcement(dto: AnnouncementDTO):
    """
    【终极工具】当公告内容在对话中生成完毕后，调用此工具提交。
    只需传入标题和版本号，正文系统会自动从对话记录中提取。
    """
    # 逻辑可以留空，因为我们会在 LangGraph 的节点里捕获它的参数
    return "SUCCESS"


# ==========================================
# LangGraph 状态与节点定义
# ==========================================

# 系统提示词：定义灵魂与模版
RELEASE_SYSTEM_PROMPT = """
你是一位资深架构师。请为 Atlas 项目撰写 Release Notes。必须依次调用获取版本和日志工具。当你获取到版本和日志后，必须立刻且仅调用 submit_announcement 工具，严禁进行任何额外的文本回复。
调用 submit_announcement 时，必须完整填写所有参数（title, version, description, content），不得遗漏。

# ⚡ 核心指令 (STRICT)
- 直接以 '# Atlas [新版本号] 发版说明' 开头，禁止输出任何推理过程、解释或开场白。
- 每个主要模块之间必须使用 '---' (水平分割线) 进行视觉隔离。
- 严禁在输出中包含诸如 '由于提到...所以升级版本' 之类的内心独白。

# ⚡ 发版流程 (严格执行)
1. 首先，在回复中直接输出完整的 Markdown 格式发版公告正文。
2. 紧接着，调用 submit_announcement 工具，仅需传入标题(title)和版本号(version)。
3. 禁止将正文内容放入工具参数中，以防传输错误。

# ⚖️ 版本规则
- [New]/[Feature] -> 升级 Minor (v2.6.0 -> v2.7.0)
- 仅 [Fix]/[Opt]/[Refactor] -> 升级 Patch (v2.6.0 -> v2.6.1)

# 📝 Markdown 结构规范 (严格遵守)
# Atlas [新版本号] 发版说明

> **发布摘要**：[一句话概括核心价值]

---
### 🚀 核心新特性
#### 1. [特性名称]
- **技术实现**：[描述]
- **功能亮点**：[描述]

---
### 🛠️ 功能优化
| 模块 | 优化内容 | 性能提升 |
| :--- | :--- | :--- |
| [模块名] | [内容] | [数据] |

---
### 🐞 缺陷修复
- [x] [修复项 A]
- [x] [修复项 B]

---
### ⚠️ 升级建议
1. **[步骤]**：[描述]
```bash
[代码块]

如有任何疑问，请联系系统管理员或在 GitHub 提交 Issue。
"""


# 这是 Agent 的“短时记忆”仓库 LangGraph 的节点之间是不直接通信的，它们唯一的共同点就是都能读写这个AgentState
# 在对话中，我们需要保留完整的历史记录。operator.add 告诉系统：当节点返回新的消息时，
# 请把它**追加（Append）**到原有的列表末尾，而不是替换掉它。这样 AI 才能记得它上一轮调用了什么工具。
class AgentState(TypedDict):
    messages: Annotated[List[BaseMessage], operator.add]


def create_release_agent():
    workflow = StateGraph(AgentState)

    # 这是 Agent 的“大脑”。
    def call_model(state: AgentState):
        llm = ChatTongyi(
            api_key=os.getenv("DASHSCOPE_API_KEY"),
            model_name="qwen-max",
            streaming=False,
            temperature=0,
            callbacks=[StreamingStdOutCallbackHandler()] # 直接输出到控制台
        )
        # 绑定工具
        tools = [get_current_version, get_git_logs, submit_announcement]
        # 告诉模型，“你有这两个技能可以用”
        model_with_tools = llm.bind_tools(tools)
        # 构造发送给 AI 的完整对话列表
        # SystemMessage表示系统级别的规章制度
        # state["messages"]: 之前的对话。如果刚才已经调过一次工具了，结果也会在这个列表里
        msg = [SystemMessage(content=RELEASE_SYSTEM_PROMPT)] + state["messages"]

        print(f"\n{PURPLE}🔮 [Agent] 大脑正在高速运转...{RESET}\n")
        print("-" * 30)

        # 让 AI 基于规章制度和历史对话思考并给出一个回应
        # 它输出的不是普通文本，而是以下两种情况之一
        # 1、“指令”：它返回一个特殊的信号，说：“我决定调用 get_git_logs，参数是 days=7
        # 2、“结果”：如果它发现资料已经全了，它会直接返回
        response = model_with_tools.invoke(msg)

        print(f"\n" + "-" * 30)
        return {"messages": [response]}

    # 这是 Agent 的“手脚”。它检查模型发出的指令。在你的电脑上真正调用工具，把结果封装成 ToolMessage
    def call_tool(state: AgentState):
        tools_map = {
            "get_current_version": get_current_version,
            "get_git_logs": get_git_logs,
            "submit_announcement": submit_announcement
        }
        last_msg = state["messages"][-1]
        outputs = []
        for call in last_msg.tool_calls:
            print(f"📡 正在执行工具: {call['name']}...")
            res = tools_map[call["name"]].invoke(call["args"])
            outputs.append(ToolMessage(tool_call_id=call["id"], content=str(res)))
        return {"messages": outputs}

    # 归档节点
    def save_db_node(state: AgentState):
        last_msg = state["messages"][-1]
        # 寻找提交指令
        submit_call = next((c for c in last_msg.tool_calls if c["name"] == "submit_announcement"), None)
        if not submit_call:
            return state

        raw_args = submit_call["args"]  # 这里已经是 AI 自动转码好的字典了
        # 兼容性提取：如果存在 'dto' key 就取它，否则取 raw_args 本身
        dto_data = raw_args.get("dto", raw_args)
        # 完整的 JSON
        full_payload = {
            "title": dto_data.get("title", "未命名"),
            "version": dto_data.get("version", "v1.0.0"),
            "description": dto_data.get("description", "暂无摘要"),
            "content": last_msg.content,  # 这里是抓取到的长文本
            "type": "RELEASE",  # 默认值
            "status": "PUBLISHED",  # 默认值
            "priority": 10  # 默认值
        }

        print(f"💾 [SaveNode] 正在向 Atlas 后端推送数据...")
        print(f"   >> 标题: {full_payload['title']}")
        print(f"   >> 版本: {full_payload['version']}")
        print(f"   >> 正文长度: {len(full_payload['content'])} 字符")

        # --- 开始调用接口 ---
        url = "http://localhost:9099/announcement/create"  # 你的 Java 后端地址
        print(f"📡 [SaveNode] 正在发起 POST 请求至: {url}")
        try:
            with httpx.Client(timeout=10.0) as client:
                response = client.post(url, json=full_payload)
                if response.status_code == 200 and response.json().get("code") == 0:
                    status_report = f"{GREEN}SUCCESS{RESET}"
                    result_msg = "SUCCESS"
                else:
                    status_report = f"{RED}FAILED ({response.status_code}){RESET}"
                    result_msg = f"ERROR: {response.text}"
        except Exception as e:
            status_report = f"{RED}NETWORK ERROR{RESET}"
            result_msg = str(e)


        # --- 🌟 任务完成报告 (Console Output) ---
        print("\n" + "=" * 60)
        print(f"{BOLD}{CYAN}🚀 ATLAS RELEASE AGENT - MISSION REPORT{RESET}")
        print("=" * 60)
        print(f"{BLUE}📌 公告标题:{RESET}  {full_payload['title']}")
        print(f"{BLUE}🔢 目标版本:{RESET}  {GREEN}{full_payload['version']}{RESET}")
        print(f"{BLUE}📝 正文长度:{RESET}  {len(full_payload['content'])} 字符")
        print(f"{BLUE}🌐 推送地址:{RESET}  {url}")
        print("-" * 60)
        print(f"{BOLD}📊 最终状态:  {status_report}{RESET}")
        print("=" * 60 + "\n")

        feedback_msg = ToolMessage(
            tool_call_id=submit_call["id"],
            content=result_msg
        )

        # 返回新的消息，将其追加到 state["messages"]
        return {"messages": [feedback_msg]}

    # 路由逻辑 检查大脑（model）输出的最后一条消息。如果消息里包含“调用工具”的请求，就指引流程走向 tools 节点；如果没有，说明任务完成，走向 end
    def router(state: AgentState):
        last_msg = state["messages"][-1]

        print(f"DEBUG: Router 正在判断，最后一条消息是: {last_msg}")

        # 检查历史记录中是否已经存在 submit_announcement 的调用
        # 这一步是为了防止 AI 在提交完后说“公告已生成”时，router 把它踢回 agent
        has_submitted = any(
            (isinstance(m, AIMessage) and m.tool_calls and any(
                tc['name'] == 'submit_announcement' for tc in m.tool_calls))
            for m in state["messages"]
        )

        # 如果最后一条消息包含提交指令，去落库
        if last_msg.tool_calls:
            for call in last_msg.tool_calls:
                if call["name"] == "submit_announcement":
                    return "save_db"
            return "tools"  # 调的是其他工具（查日志等）

        # 如果没有工具调用了，但我们已经提交过了，说明任务彻底完成
        if has_submitted:
            return "save_db"

        # 兜底：既没提交，也没工具调用，才回 agent 思考
        return "agent"

    # 组装节点与边
    workflow.add_node("agent", call_model)
    workflow.add_node("tools", call_tool)
    workflow.add_node("save_db", save_db_node)
    # 设置起点
    workflow.set_entry_point("agent")
    # 这一步决定了大脑思考完后，是去执行工具，还是去落库
    workflow.add_conditional_edges(
        "agent",
        router,
        {
            "tools": "tools",
            "save_db": "save_db",
            "agent": "agent"
        }
    )
    # 为 tools 节点增加出口 执行完工具（比如拿到了日志）后，必须回到 agent 让它继续写公告
    workflow.add_edge("tools", "agent")
    # 设置终点
    workflow.add_edge("save_db", END)  # 唯一的出口

    return workflow.compile()


if __name__ == "__main__":
    load_dotenv()

    print(f"\n{BOLD}{CYAN}🚀 Atlas Release Agent 启动...{RESET}")

    app = create_release_agent()

    # 模拟用户指令
    input_msg = {"messages": [HumanMessage(content="生成最近 7 天的发版说明")]}

    for event in app.stream(input_msg, stream_mode="updates"):
        for node, value in event.items():
            print(f"\n{PURPLE}--- [节点结束]: {node} ---{RESET}")
