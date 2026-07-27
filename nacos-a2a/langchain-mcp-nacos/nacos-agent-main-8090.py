"""AgentScope 2.x + agentscope-runtime A2A/Nacos example.

AgentScope Runtime's ``framework="agentscope"`` adapter still targets the
1.x ``(Msg, last)`` / ``tool_use`` APIs and cannot import against 2.x, so this
file uses ``framework="text"`` and streams ``TEXT_BLOCK_DELTA`` events from
``Agent.reply_stream``.
"""

from __future__ import annotations

import os
from typing import Any

from a2a.types import AgentCapabilities, AgentCard
from agentscope.agent import Agent
from agentscope.credential import OpenAICredential
from agentscope.event import EventType
from agentscope.message import TextBlock, ToolResultState, UserMsg
from agentscope.model import OpenAIChatModel
from agentscope.permission import PermissionBehavior, PermissionDecision
from agentscope.state import AgentState
from agentscope.tool import ToolBase, Toolkit, ToolChunk
from agentscope_runtime.engine.app import AgentApp
from agentscope_runtime.engine.deployers.adapter.a2a import (
    AgentCardWithRuntimeConfig,
)
from agentscope_runtime.engine.deployers.adapter.a2a.nacos_a2a_registry import (
    NacosRegistry,
)
from agentscope_runtime.engine.schemas.agent_schemas import AgentRequest
from v2.nacos import ClientConfigBuilder

## Agent port
port = 8090


class WeatherTool(ToolBase):
    """Look up the latest weather for a region."""

    name = "weather"
    description = "查看某个地区的最新天气情况"
    input_schema = {
        "type": "object",
        "properties": {
            "local": {
                "type": "string",
                "description": "完整的地区名称",
            },
        },
        "required": ["local"],
    }
    is_concurrency_safe = True
    is_read_only = True

    async def check_permissions(
        self,
        tool_input: dict[str, Any],
        context: Any,
    ) -> PermissionDecision:
        return PermissionDecision(
            behavior=PermissionBehavior.ALLOW,
            message="Weather lookup is read-only.",
        )

    async def call(self, local: str) -> ToolChunk:
        return ToolChunk(
            content=[
                TextBlock(
                    text=f"{local}，地区的最新天气为多云转晴，30-35摄氏度。",
                ),
            ],
            state=ToolResultState.SUCCESS,
        )


def _build_model() -> OpenAIChatModel:
    api_key = os.getenv("OPENAI_API_KEY", "")
    base_url = os.getenv("OPENAI_BASE_URL")
    model_name = os.getenv("OPENAI_MODEL", "Qwen/Qwen3.5-27B")

    return OpenAIChatModel(
        credential=OpenAICredential(
            api_key=api_key,
            base_url=base_url,
        ),
        model=model_name,
        stream=True,
        parameters=OpenAIChatModel.Parameters(thinking_enable=False),
    )


def _request_to_user_msgs(request: AgentRequest) -> list:
    """Convert runtime AgentRequest.input into AgentScope 2.x UserMsg list."""
    user_msgs = []
    for message in request.input or []:
        texts: list[str] = []
        for content in message.content or []:
            text = getattr(content, "text", None)
            if text:
                texts.append(text)
        if not texts:
            continue
        name = getattr(message, "name", None) or message.role or "user"
        user_msgs.append(UserMsg(name, "\n".join(texts)))
    return user_msgs


## Register tools
toolkit = Toolkit(tools=[WeatherTool()])

## Create agent (2.x unified Agent; no formatter / InMemoryMemory)
my_agent = Agent(
    name="xiaobichao",
    system_prompt="你叫小毕超，是一个天气智能助手",
    model=_build_model(),
    toolkit=toolkit,
    state=AgentState(),
)

## Create Nacos Registry
registry = NacosRegistry(
    nacos_client_config=ClientConfigBuilder()
    .server_address("localhost:8848")
    .username("nacos")
    .password("nacos")## Nacos address
    .build(),
)

## Declare AgentCard
agent_card = AgentCard(
    name="xiaobichao",
    description="小毕超天气智能助手",
    version="1.0.0",
    url=f"http://localhost:{port}",
    capabilities=AgentCapabilities(
        push_notifications=False,
        state_transition_history=True,
        streaming=True,
    ),
    default_input_modes=["text/plain"],
    default_output_modes=["text/plain"],
    skills=[],
)

## Create AgentApp (A2A + Nacos still from agentscope-runtime)
app = AgentApp(
    app_name="xiaobichao",
    app_description="小毕超天气智能助手",
    a2a_config=AgentCardWithRuntimeConfig(
        port=port,
        registry=registry,
        agent_card=agent_card,
    ),
)


@app.query(framework="agentscope")
async def query_func(
    self,
    request: AgentRequest = None,
    **kwargs,
):
    """Stream assistant text deltas from AgentScope 2.x reply_stream."""
    del self, kwargs  # unused; signature kept for AgentApp injection
    user_msgs = _request_to_user_msgs(request)
    if not user_msgs:
        yield "请输入有效的文本消息。"
        return

    async for event in my_agent.reply_stream(user_msgs):
        if getattr(event, "type", None) == EventType.TEXT_BLOCK_DELTA:
            delta = getattr(event, "delta", None)
            if delta:
                yield delta


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=port)
