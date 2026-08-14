export interface Series {
  label: string;
  color: string;
  data: number[];
}

interface Props {
  series: Series[];
  /** y축 최대값. 미지정 시 데이터에서 계산 */
  yMax?: number;
  /** y축 단위 접미사 (예: '%') */
  unit?: string;
  height?: number;
}

/**
 * 의존성 없는 인라인 SVG 라인 차트.
 * 시계열 메트릭(메모리/CPU/요청수 등)을 표시한다.
 */
export function LineChart({ series, yMax, unit = '', height = 180 }: Props) {
  const W = 600;
  const H = height;
  const padL = 40;
  const padB = 22;
  const padT = 12;
  const padR = 12;

  const allValues = series.flatMap((s) => s.data);
  const maxRaw = yMax ?? Math.max(1, ...allValues);
  const max = maxRaw <= 0 ? 1 : maxRaw;
  const maxLen = Math.max(...series.map((s) => s.data.length), 1);

  const plotW = W - padL - padR;
  const plotH = H - padT - padB;

  const x = (i: number) => padL + (maxLen <= 1 ? 0 : (i / (maxLen - 1)) * plotW);
  const y = (v: number) => padT + plotH - (v / max) * plotH;

  const gridLines = [0, 0.25, 0.5, 0.75, 1];

  return (
    <div style={{ overflowX: 'auto' }}>
      <svg viewBox={`0 0 ${W} ${H}`} width="100%" height={H} preserveAspectRatio="none" role="img">
        {/* 그리드 + y축 라벨 */}
        {gridLines.map((g) => {
          const yy = padT + plotH - g * plotH;
          return (
            <g key={g}>
              <line x1={padL} y1={yy} x2={W - padR} y2={yy} stroke="#eef0f3" strokeWidth={1} />
              <text x={padL - 6} y={yy + 3} textAnchor="end" fontSize={10} fill="#98a1ad">
                {Math.round(max * g)}
                {unit}
              </text>
            </g>
          );
        })}

        {/* 라인 */}
        {series.map((s) => {
          if (s.data.length === 0) return null;
          const points = s.data.map((v, i) => `${x(i)},${y(v)}`).join(' ');
          const last = s.data[s.data.length - 1];
          return (
            <g key={s.label}>
              <polyline points={points} fill="none" stroke={s.color} strokeWidth={2} strokeLinejoin="round" />
              {s.data.length > 0 && <circle cx={x(s.data.length - 1)} cy={y(last)} r={3} fill={s.color} />}
            </g>
          );
        })}
      </svg>

      {/* 범례 */}
      <div className="row" style={{ gap: 16, padding: '4px 8px 0', flexWrap: 'wrap' }}>
        {series.map((s) => {
          const last = s.data.length ? s.data[s.data.length - 1] : 0;
          return (
            <span key={s.label} style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontSize: 12 }}>
              <span style={{ width: 10, height: 10, borderRadius: 2, background: s.color, display: 'inline-block' }} />
              {s.label}: <b>{Math.round(last)}{unit}</b>
            </span>
          );
        })}
      </div>
    </div>
  );
}
