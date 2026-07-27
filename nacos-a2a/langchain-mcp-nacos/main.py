from agentscope.agent import ReActAgent
from agentscope.model import OpenAIChatModel
from agentscope.formatter import DashScopeChatFormatter
from agentscope.memory import InMemoryMemory
from agentscope.pipeline import stream_printing_messages
from agentscope_runtime.engine.app import AgentApp
from agentscope_runtime.engine.schemas.agent_schemas import AgentRequest
from agentscope_runtime.engine.deployers.adapter.a2a import AgentCardWithRuntimeConfig
from agentscope_runtime.engine.deployers.adapter.a2a.nacos_a2a_registry import NacosRegistry
from v2.nacos import ClientConfigBuilder
from a2a.types import AgentCard, AgentCapabilities
from agentscope.tool import Toolkit, ToolResponse
from agentscope.message import TextBlock, ToolUseBlock

## Agent端口
port = 8090

## 定义测试工具
def weather(local: str) -> ToolResponse:
    '''查看某个地区的最新天气情况
    Args:
        local(str):完整的地区名称
    '''
    return ToolResponse(
        content=[
            TextBlock(
                type="text",
                text=f"{local}，地区的最新天气为多云转晴，30-35摄氏度。"
            )
        ]
    )

## 注册工具
toolkit = Toolkit()
toolkit.register_tool_function(weather)

## 创建智能体
my_agent = ReActAgent(
    name="xiaobichao",
    sys_prompt="你叫小毕超，是一个天气智能助手",
    model=OpenAIChatModel(
        model_name="Qwen/Qwen3.5-27B",
        stream=True,
        enable_thinking=False
    ),
    formatter=DashScopeChatFormatter(),
    toolkit=toolkit,
    memory=InMemoryMemory(),
)

## 创建 Nacos Registry
registry = NacosRegistry(
    nacos_client_config=ClientConfigBuilder()
    .server_address("localhost:8848") ## Nacos地址
    .build()
)

## 声明AgentCard
agent_card = AgentCard(
    name="xiaobichao",
    description="小毕超天气智能助手",
    version="1.0.0",
    url=f"http://localhost:{port}",
    capabilities=AgentCapabilities(
        push_notifications=False,
        state_transition_history=True,
        streaming=True
    ),
    default_input_modes=["text/plain"],
    default_output_modes=["text/plain"],
    skills=[]
)

## 创建AgentApp
app = AgentApp(
    app_name="xiaobichao",
    app_description="小毕超天气智能助手",
    a2a_config=AgentCardWithRuntimeConfig(
        port=port,
        registry=registry,
        agent_card=agent_card,
    ),
)

#定义执行逻辑
@app.query(framework="agentscope")
async def query_func(self, msgs, request: AgentRequest = None, **kwargs):
    session_id = request.session_id
    user_id = request.user_id
    async for msg, last in stream_printing_messages(agents=[my_agent], coroutine_task=my_agent(msgs), ):
        yield msg, last


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=port)
