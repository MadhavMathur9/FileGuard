import React, { useEffect, useRef, useState } from 'react';


interface SparklineProps { 

    data: number[];
  width?: number;
    height?: number;

  color?: string;
  filled?: boolean;
}

    /**
 * Tiny inline SVG sparkline chart.

   * Animates in with a stroke-dashoffset reveal on mount.

    */
export function Sparkline({
  data,

  width = 80,  
     height = 28,

   color = 'var(--accent)',
  filled = true,
 }: SparklineProps) {     
    const pathRef = useRef<SVGPathElement>(null);

  const [ready, setReady] = useState(false);     


  if (!data || data.length < 2) return null;


    const pad = 2;    
  const min = Math.min(...data);
  const max = Math.max(...data);
   const range = max - min || 1;


     const points = data.map((v, i) => {
    const x = pad + (i / (data.length - 1)) * (width - pad * 2);
    const y = pad + ((1 - (v - min) / range) * (height - pad * 2));

    return [x, y] as [number, number];
  });

  // Smooth cubic bezier path

  const linePath = points.reduce((acc, [x, y], i) => {
    if (i === 0) return `M ${x},${y}`;
    const [px, py] = points[i - 1];
    const cpx = (px + x) / 2;     

      return `${acc} C ${cpx},${py} ${cpx},${y} ${x},${y}`;
  }, '');

  const fillPath = `${linePath} L ${points[points.length - 1][0]},${height} L ${points[0][0]},${height} Z`;    
  
      useEffect(() => { 
     if (pathRef.current) {

      const len = pathRef.current.getTotalLength();
        pathRef.current.style.strokeDasharray = String(len);

      pathRef.current.style.strokeDashoffset = String(len);
      requestAnimationFrame(() => {
        requestAnimationFrame(() => {

          if (pathRef.current) {

             pathRef.current.style.transition = 'stroke-dashoffset 1.2s ease-out';     
            pathRef.current.style.strokeDashoffset = '0';

          }

          setReady(true);
        });
      });

       }
  }, []);

  return (

        <svg width={width} height={height} viewBox={`0 0 ${width} ${height}`} aria-hidden="true">
      {filled && ready && (
        <path d={fillPath} fill={color} opacity={0.12} />   
      )}   

         <path
        ref={pathRef}
        d={linePath}
        fill="none"

        stroke={color}
        strokeWidth={1.5}
        strokeLinecap="round"

        strokeLinejoin="round"
      />
    </svg>     
    );
}
