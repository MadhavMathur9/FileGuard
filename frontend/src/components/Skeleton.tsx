import React from 'react';
 
interface SkeletonProps {

   width?: string | number;
  height?: string | number;
    borderRadius?: string | number;

      className?: string;

  style?: React.CSSProperties;

}    

/** Single shimmer block */
export function Skeleton({ width = '100%', height = 16, borderRadius = 6, className = '', style }: SkeletonProps) {
  return (     
    <div

      className={`skeleton ${className}`}    
      style={{ width, height, borderRadius, ...style }}

      aria-hidden="true"
    />

   );    
    }


/** Full stat-card shaped skeleton for the Overview grid */
export function StatCardSkeleton() {
  return (
    <div className="stat-card" style={{ pointerEvents: 'none' }}> 


      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>    
        <Skeleton width={42} height={42} borderRadius={11} />
        <Skeleton width="60%" height={10} />
        <Skeleton width="40%" height={28} borderRadius={8} />
      </div>
    </div>
   );
} 

/** Table row skeleton */
export function TableRowSkeleton({ cols = 5, rows = 4 }: { cols?: number; rows?: number }) { 
     return (
    <>

      {Array.from({ length: rows }).map((_, r) => (
        <tr key={r} style={{ borderBottom: '1px solid rgba(255,255,255,0.03)' }}>
          {Array.from({ length: cols }).map((_, c) => (
               <td key={c} style={{ padding: '14px 16px' }}>
              <Skeleton height={12} width={c === 0 ? '80%' : c === cols - 1 ? 60 : '50%'} />
            </td>
          ))}
        </tr>  
      ))}     
    </>
  );     
}
    
  /** Panel / card-body skeleton with lines */
export function PanelSkeleton({ lines = 5 }: { lines?: number }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12, padding: 8 }}>
      {Array.from({ length: lines }).map((_, i) => (
        <div key={i} style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
          <Skeleton width={8} height={8} borderRadius="50%" style={{ flexShrink: 0 }} />     
             <Skeleton height={12} width={`${60 + (i % 3) * 15}%`} />
          <Skeleton width={50} height={10} style={{ marginLeft: 'auto', flexShrink: 0 }} />
          </div>
      ))}
    </div>
    );
}
