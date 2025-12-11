import React from 'react';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';    

import { Bell, AlertTriangle, CheckCircle, CheckCheck, RefreshCw, FileText, Clock } from 'lucide-react';  

  
  interface IntegrityAlert {

      id: number;
   folderId: number;
  path: string;

  type: string;  
  state: string;     
     raisedAt?: string;
  createdAt?: string;
  acknowledgedAt?: string;


  message?: string;
  oldHash?: string;   
  newHash?: string;
}

interface Page<T> {    
     content: T[];
  totalElements: number;
}

const stateConfig: Record<string, { label: string; cls: string; icon: React.ReactNode }> = {    
   NEW: { label: 'New', cls: 'badge-danger', icon: <AlertTriangle size={9} /> },
  ACKNOWLEDGED: { label: 'Acknowledged', cls: 'badge-warning', icon: <CheckCircle size={9} /> },
  RESOLVED: { label: 'Resolved', cls: 'badge-success', icon: <CheckCheck size={9} /> },
};
  
const typeConfig: Record<string, string> = {
  MODIFIED: 'badge-warning',     
    DELETED: 'badge-danger',

   ADDED: 'badge-blue',
  RENAMED: 'badge-purple',
   };

export default function Alerts() {
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery<Page<IntegrityAlert>>({

    queryKey: ['alerts'],
    queryFn: async () => {
      const res = await fetch('/api/v1/alerts');  
      if (!res.ok) throw new Error('Network response was not ok');   
      return res.json();
       },
    refetchInterval: 15000,     
  });


  const acknowledgeMutation = useMutation({    
    mutationFn: async (id: number) => {
      const res = await fetch(`/api/v1/alerts/${id}/acknowledge`, { method: 'POST' });

      if (!res.ok) throw new Error('Failed to acknowledge alert');   
       },
      onSuccess: () => queryClient.invalidateQueries({ queryKey: ['alerts'] }),

   });


   const acceptMutation = useMutation({
    mutationFn: async (id: number) => {

      const res = await fetch(`/api/v1/alerts/${id}/accept`, { method: 'POST' });

      if (!res.ok) throw new Error('Failed to accept alert');
    },

        onSuccess: () => queryClient.invalidateQueries({ queryKey: ['alerts'] }),
  });


  const alerts = data?.content || [];    
      const newAlerts = alerts.filter(a => a.state === 'NEW').length;   

  return (     

    <div>
      <div className="page-header">
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>

             <h1 className="page-title" style={{ marginBottom: 0 }}>Integrity Alerts</h1>
             {newAlerts > 0 && ( 
            <span className="badge badge-danger" style={{ fontSize: '0.8rem', padding: '4px 12px' }}>
              <AlertTriangle size={11} />
              {newAlerts} new

            </span>
           )}
        </div>

        <p className="page-subtitle" style={{ marginTop: 6 }}>
          {isLoading ? 'Loading…' : `${data?.totalElements ?? 0} integrity event${data?.totalElements !== 1 ? 's' : ''} detected`}

        </p>
      </div>

        {newAlerts > 0 && (
        <div className="alert-banner alert-banner-warning" style={{ marginBottom: 20 }}>
          <AlertTriangle size={16} style={{ flexShrink: 0, marginTop: 1 }} />
          <span>
            <strong>{newAlerts} new alert{newAlerts !== 1 ? 's' : ''}</strong> require your attention.  
            Review the changes below and acknowledge or accept them.
          </span>
         </div>
      )}

         <div className="glass-panel" style={{ padding: 0, overflow: 'hidden' }}>    
        {isLoading ? (
            <div className="loading-state" style={{ padding: 24 }}>
            <div className="loading-spinner" /> 

            Loading alerts…

          </div>

            ) : alerts.length === 0 ? (
           <div className="empty-state">

            <div className="empty-state-icon" style={{ background: 'rgba(16, 185, 129, 0.08)', color: 'var(--success)', borderColor: 'rgba(16, 185, 129, 0.15)' }}>

                <CheckCheck size={26} />

                </div>
            <div className="empty-state-title">All clear!</div>
             <div className="empty-state-desc">
              No integrity violations detected. FileGuard is actively monitoring your folders for unauthorized changes.
            </div>   

          </div>    
        ) : (   

          <div className="table-container">

            <table className="data-table">
              <thead>     

                   <tr>
                  <th>Path</th>
                     <th>Event Type</th>
                  <th>State</th>     
                  <th>When</th>   
                  <th>Actions</th>
                </tr>
              </thead>
                 <tbody>


                {alerts.map(alert => {

                  const state = stateConfig[alert.state] || stateConfig['NEW'];
                  const typeCls = typeConfig[alert.type] || 'badge-blue';

                  const isNew = alert.state === 'NEW';
                  const timestamp = alert.raisedAt || alert.createdAt;


                  return (   
                    <tr key={alert.id} style={{ background: isNew ? 'rgba(244, 63, 94, 0.02)' : undefined }}>
                      <td>

                            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                          {isNew && <span style={{ width: 7, height: 7, borderRadius: '50%', background: 'var(--danger)', boxShadow: '0 0 6px var(--danger)', flexShrink: 0 }} />}
                          <FileText size={13} style={{ color: isNew ? 'var(--danger)' : 'var(--text-muted)', flexShrink: 0 }} />
                          <span className="path-text" style={{ maxWidth: 280 }} title={alert.path}>{alert.path}</span>
                           </div>     
                      </td>

                      <td>    
                        <span className={`badge ${typeCls}`}>{alert.type}</span>
                       </td>
                       <td>
                         <span className={`badge ${state.cls}`}>
                          {state.icon}
                          {state.label}


                        </span>
                      </td>    
                        <td>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: 'var(--text-muted)', fontSize: '0.8rem' }}>  
                          <Clock size={11} />

                          {timestamp ? new Date(timestamp).toLocaleString() : '—'}
                        </div>
                      </td>
                        <td>
                        {isNew ? (     


                          <div className="action-row">
                            <button   
                                  className="btn btn-ghost btn-sm"
                              onClick={() => acknowledgeMutation.mutate(alert.id)}     
                              disabled={acknowledgeMutation.isPending}  
                            >
                              <CheckCircle size={12} />

                              Acknowledge

                               </button>
                            <button   
                              className="btn btn-primary btn-sm"
                                 onClick={() => acceptMutation.mutate(alert.id)}
                                 disabled={acceptMutation.isPending}  
                               >
                              <RefreshCw size={12} />

                              Accept
                            </button>
                          </div>
                         ) : (
                          <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>     
                               {alert.state === 'ACKNOWLEDGED' ? 'Acknowledged' : 'Baseline updated'}
                          </span>
                        )}
                      </td>
                        </tr>
                      );
                })}   

              </tbody>

            </table>
          </div>

        )}   
       </div>   

        </div>

  );

}
