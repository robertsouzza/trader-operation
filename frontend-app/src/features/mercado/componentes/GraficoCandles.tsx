import { useEffect, useRef } from 'react';
import {
  CandlestickSeries,
  createChart,
  type CandlestickData,
  type IChartApi,
  type IPriceLine,
  type ISeriesApi,
  type Time,
} from 'lightweight-charts';
import type { Candle } from '../api/mercadoApi';

export interface LinhaOperacao {
  readonly preco: number;
  readonly cor: string;
  readonly rotulo: string;
}

interface Props {
  readonly candles: readonly Candle[];
  readonly linhas?: readonly LinhaOperacao[];
  readonly altura?: number;
}

const CORES_TEMA = {
  fundo: '#0f172a',
  texto: '#e2e8f0',
  grid: '#334155',
  borda: '#475569',
  alta: '#22c55e',
  baixa: '#ef4444',
} as const;

function emSegundosUTC(iso: string): Time {
  return Math.floor(new Date(iso).getTime() / 1000) as Time;
}

function paraCandlestickData(c: Candle): CandlestickData {
  return {
    time: emSegundosUTC(c.ts),
    open: c.abertura,
    high: c.maxima,
    low: c.minima,
    close: c.fechamento,
  };
}

export function GraficoCandles({ candles, linhas = [], altura = 480 }: Props) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const chartRef = useRef<IChartApi | null>(null);
  const seriesRef = useRef<ISeriesApi<'Candlestick'> | null>(null);
  const linhasRef = useRef<IPriceLine[]>([]);

  // Monta o gráfico uma vez; destrói no unmount.
  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    const chart = createChart(container, {
      width: container.clientWidth,
      height: altura,
      layout: {
        background: { color: CORES_TEMA.fundo },
        textColor: CORES_TEMA.texto,
      },
      grid: {
        vertLines: { color: CORES_TEMA.grid },
        horzLines: { color: CORES_TEMA.grid },
      },
      rightPriceScale: { borderColor: CORES_TEMA.borda },
      timeScale: {
        borderColor: CORES_TEMA.borda,
        timeVisible: true,
        secondsVisible: false,
      },
    });

    const series = chart.addSeries(CandlestickSeries, {
      upColor: CORES_TEMA.alta,
      downColor: CORES_TEMA.baixa,
      wickUpColor: CORES_TEMA.alta,
      wickDownColor: CORES_TEMA.baixa,
      borderVisible: false,
    });

    chartRef.current = chart;
    seriesRef.current = series;

    const observer = new ResizeObserver((entradas) => {
      const entrada = entradas[0];
      if (!entrada) return;
      chart.applyOptions({ width: entrada.contentRect.width });
    });
    observer.observe(container);

    return () => {
      observer.disconnect();
      chart.remove();
      chartRef.current = null;
      seriesRef.current = null;
      linhasRef.current = [];
    };
  }, [altura]);

  // Atualiza os dados sempre que candles muda.
  useEffect(() => {
    const series = seriesRef.current;
    if (!series) return;
    series.setData(candles.map(paraCandlestickData));
    chartRef.current?.timeScale().fitContent();
  }, [candles]);

  // Troca as linhas de preço (entrada/stop/alvos) quando `linhas` muda.
  useEffect(() => {
    const series = seriesRef.current;
    if (!series) return;
    for (const linha of linhasRef.current) {
      try {
        series.removePriceLine(linha);
      } catch {
        // ignora
      }
    }
    linhasRef.current = linhas.map((l) =>
      series.createPriceLine({
        price: l.preco,
        color: l.cor,
        lineWidth: 2,
        lineStyle: 0, // Solid
        axisLabelVisible: true,
        title: l.rotulo,
      }),
    );
  }, [linhas]);

  return <div ref={containerRef} className="w-full" style={{ height: altura }} />;
}
