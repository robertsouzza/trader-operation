FROM python:3.12.8-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    PIP_NO_CACHE_DIR=1

RUN addgroup --system --gid 1000 motor && adduser --system --uid 1000 --gid 1000 motor

WORKDIR /app

COPY pyproject.toml README.md ./
COPY src ./src
RUN pip install .

COPY data ./data

USER motor
EXPOSE 8091

CMD ["uvicorn", "motor_quant.main:criar_app", "--factory", "--host", "0.0.0.0", "--port", "8091"]
