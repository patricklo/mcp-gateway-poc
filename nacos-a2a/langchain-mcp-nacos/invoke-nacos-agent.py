import asyncio
import httpx
from v2.nacos import ClientConfig
from a2a.types import AgentCard

NACOS = "http://localhost:8848"
USERNAME = "nacos"
PASSWORD = "nacos"  # 改成你实际密码（3.x 常已不是默认 nacos）

async def main():
    async with httpx.AsyncClient(base_url=NACOS) as client:
        login = await client.post(
            "/nacos/v3/auth/user/login",
            data={"username": USERNAME, "password": PASSWORD},
        )
        login.raise_for_status()
        token = login.json()["accessToken"]
        for i in range(10):
            r = await client.get(
                "/nacos/v3/admin/ai/a2a",
                params={
                    "namespaceId": "public",
                    "agentName": "xiaobichao",
                },
                headers={"Authorization": f"Bearer {token}"},
            )
            print(r.status_code, r.text)  # 调试时先看原始响应
            r.raise_for_status()
            data = r.json()
            payload = data.get("data") or data
            card = AgentCard.model_validate(payload)
            print(card.url)

if __name__ == "__main__":
    asyncio.run(main())