
import React, { useEffect, useState } from 'react';

    import { useQuery } from '@tanstack/react-query';
 import {
  Files, HardDrive, Copy, Trash2, Activity,

      CheckCircle2, Zap, Clock,
} from 'lucide-react';
import { useAnimatedCounter } from '../hooks/useAnimatedCounter';   

 import { StatCardSkeleton, PanelSkeleton } from '../components/Skeleton';     

 import { Sparkline } from '../components/Sparkline';  
  
interface Stats {
  totalFiles: number;
     totalSize: number;   
  duplicateGroups: number;
  wastedBytes: number;   
}


  interface LiveEvent {
  id: number;    
  message: string;

  type: 'info' | 'duplicate' | 'hash'; 
   time: string;
    }     


const formatBytes = (bytes: number) => {

  if (!bytes || bytes === 0) return '0 B';   
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
};


const formatTime = () =>

   new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', second: '2-digit' });

   /** Stat card with animated counter + sparkline */
  function StatCard({
  label, rawValue, displayValue, icon: Icon, cls, danger, sparkData,

    }: {
  label: string;   

   rawValue: number;
  displayValue: string;
     icon: React.ElementType;

      cls: string;
  danger?: boolean;

  sparkData: number[];

}) {
  const animated = useAnimatedCounter(rawValue, 1200, rawValue > 0);
  // Format the animated number the same way as displayValue, but for counts show the counter
  const showCounter = !isNaN(rawValue) && Number.isFinite(rawValue);
   
  return (
    <div className={`stat-card stat-card-${cls}`}>
      <div className={`stat-icon stat-icon-${cls}`}> 
           <Icon size={20} />
      </div>

      <div className="stat-label">{label}</div>
          <div className={`stat-value ${danger ? 'stat-value-danger' : ''}`}>
        {showCounter && rawValue < 100000
          ? animated.toLocaleString()
          : displayValue}

        </div>

      {/* Sparkline bottom-right */}
          <div className="stat-sparkline">
        <Sparkline     
            data={sparkData}
          color={danger ? 'var(--danger)' : `var(--accent)`}
          width={72}    
          height={28}
         />
      </div>
    </div>

  );
}  

/** Timeline item */
function TimelineItem({   
    event,
}: {

  event: LiveEvent;
    }) {
     const color = event.type === 'duplicate'    

    ? 'var(--warning)'
    : event.type === 'hash'
    ? 'var(--success)'
        : 'var(--accent)';


  const icon = event.type === 'duplicate'
    ? <Copy size={9} />

    : event.type === 'hash'

    ? <CheckCircle2 size={9} />
    : <Zap size={9} />;

  return ( 
    <div className="timeline-item">    


      <div     

        className="timeline-dot"
          style={{ borderColor: color, color }}
      >
        {icon}
      </div>

      <div className="timeline-content"> 
        <div className="timeline-title">{event.message}</div>
        <div className="timeline-meta">
          <Clock size={10} style={{ display: 'inline', marginRight: 4 }} />
             {event.time}
        </div>
          </div> 

    </div>

   );
}

export default function DashboardOverview() {
   const [liveEvents, setLiveEvents] = useState<LiveEvent[]>([]);
  // Fake sparkline histories that grow as events come in
      const [sparklines, setSparklines] = useState<Record<string, number[]>>({


       files: [0, 1, 3, 2, 5, 4, 6, 5, 8, 7],
    size: [0, 2, 1, 4, 3, 6, 5, 7, 6, 9],

    dups: [0, 0, 1, 0, 2, 1, 3, 2, 3, 4],
      wasted: [0, 1, 0, 2, 1, 3, 2, 4, 3, 5],
  });

  const { data: stats, isLoading } = useQuery<Stats>({

      queryKey: ['stats'],
    queryFn: async () => {
      const res = await fetch('/api/v1/stats');
      if (!res.ok) throw new Error('Failed to load stats');
      return res.json();
    },
    refetchInterval: 10000,
  });

  useEffect(() => {
    const evtSource = new EventSource('/api/v1/events/stream');

    const addEvent = (message: string, type: LiveEvent['type']) => {

      setLiveEvents(prev => [
        { id: Date.now(), message, type, time: formatTime() },
        ...prev,
      ].slice(0, 14));

          if (type === 'hash') {
        setSparklines(prev => ({
          ...prev,    


          files: [...prev.files.slice(-9), (prev.files[prev.files.length - 1] || 0) + 1],
            }));
        }

      if (type === 'duplicate') {
        setSparklines(prev => ({
          ...prev,     
          dups: [...prev.dups.slice(-9), (prev.dups[prev.dups.length - 1] || 0) + 1],

          wasted: [...prev.wasted.slice(-9), (prev.wasted[prev.wasted.length - 1] || 0) + 1],

        }));
          }
    };

    evtSource.addEventListener('connected', () =>
      addEvent('Connected to live event stream', 'info')
    );
    evtSource.addEventListener('duplicate.found', (e) => {

      try {
        const p = JSON.parse(e.data);
        addEvent(`Duplicate cluster — hash ${p.hash?.substring(0, 8)}… (${p.count} copies)`, 'duplicate');

      } catch {}
    });
    evtSource.addEventListener('file.hashed', (e) => { 
        try {
        const p = JSON.parse(e.data);
           const name = p.path?.split(/[/\\]/).pop() || p.path;
        addEvent(`Indexed: ${name}`, 'hash');
      } catch {}
        });
  
    return () => evtSource.close();
    }, []);
    
  const statCards = [
     {
      key: 'files',

        label: 'Total Indexed Files',
      rawValue: stats?.totalFiles ?? 0,
      displayValue: isLoading ? '—' : (stats?.totalFiles ?? 0).toLocaleString(),

      icon: Files,

      cls: 'blue',    
    },
    {
      key: 'size',
      label: 'Total Size Monitored',
        rawValue: NaN, // bytes — skip counter animation
      displayValue: isLoading ? '—' : formatBytes(stats?.totalSize || 0),
      icon: HardDrive,

      cls: 'purple',    

    },
        {   
         key: 'dups',
          label: 'Duplicate Groups',
         rawValue: stats?.duplicateGroups ?? 0,
      displayValue: isLoading ? '—' : String(stats?.duplicateGroups ?? 0),

      icon: Copy,

         cls: 'warning',

    },

    {


      key: 'wasted',

      label: 'Wasted Space',

      rawValue: NaN,    

      displayValue: isLoading ? '—' : formatBytes(stats?.wastedBytes || 0),

         icon: Trash2,
      cls: 'danger',
      danger: true,   

    },
  ];

  
   return (

    <div>    

      <div className="page-header">
        <h1 className="page-title">Overview</h1>   
           <p className="page-subtitle">Real-time file system monitoring and analysis</p>
      </div>

      {/* Stats Grid */}
      <div className="stats-grid">
         {isLoading 
          ? Array.from({ length: 4 }).map((_, i) => <StatCardSkeleton key={i} />)
          : statCards.map(({ key, ...card }) => (
              <StatCard
                key={key}     


                {...card} 

                sparkData={sparklines[key] || [0]}
              />

            ))

        }    
       </div>

      {/* Live Event Feed — now as timeline */}  
      <div className="glass-panel">
            <div className="section-header">

          <div className="section-title" style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <Activity size={18} style={{ color: 'var(--accent)' }} />
            Activity Timeline
          </div>
          <span className="badge badge-blue">
               <span style={{
              width: 6, height: 6, borderRadius: '50%',     
              background: 'var(--accent)', display: 'inline-block',
              boxShadow: '0 0 5px var(--accent)',
            }} />

            Live
          </span>
        </div>


        {liveEvents.length === 0 ? (   

          <PanelSkeleton lines={4} />   


          ) : (     
              <div className="timeline">   

            {liveEvents.map(event => (
              <TimelineItem key={event.id} event={event} />
            ))}
          </div>

        )}
      </div>

    </div>
     );
}
