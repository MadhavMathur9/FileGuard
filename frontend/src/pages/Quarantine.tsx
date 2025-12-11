import React from 'react';
   import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Shield, RotateCcw, Trash2, FileText, Clock } from 'lucide-react';

interface QuarantineItem {
  id: number;
  originalPath: string;   
  quarantinePath: string;

  fullHash: string;
  reason: string;
  quarantinedAt: string;

}

    const formatBytes = (bytes: number) => {
      if (!bytes) return '0 B';   
   const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];

      const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
   };
 
   export default function Quarantine() {
  const queryClient = useQueryClient();    

   
   const { data: items, isLoading } = useQuery<QuarantineItem[]>({

    queryKey: ['quarantine'],
    queryFn: async () => {    
      const res = await fetch('/api/v1/quarantine');
      if (!res.ok) throw new Error('Network response was not ok');
      return res.json();   

    },
  });

  const restoreMutation = useMutation({
    mutationFn: async (id: number) => {
      const res = await fetch(`/api/v1/quarantine/${id}/restore`, { method: 'POST' });
      if (!res.ok) throw new Error('Failed to restore item');
     },

    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['quarantine'] });
      queryClient.invalidateQueries({ queryKey: ['stats'] }); 
    },
  }); 

  const purgeMutation = useMutation({  


     mutationFn: async (id: number) => {
          const res = await fetch(`/api/v1/quarantine/${id}`, { method: 'DELETE' }); 

      if (!res.ok) throw new Error('Failed to purge item');
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['quarantine'] }),     
  });   
  
  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Quarantine</h1>
        <p className="page-subtitle">
              {isLoading ? 'Loading…' : `${items?.length ?? 0} file${items?.length !== 1 ? 's' : ''} in safe quarantine`}
        </p>

      </div>
    
      <div className="glass-panel" style={{ padding: 0, overflow: 'hidden' }}>
        {isLoading ? (
          <div className="loading-state" style={{ padding: 24 }}>

            <div className="loading-spinner" />
            Loading quarantine…
          </div>    
        ) : !items || items.length === 0 ? (

          <div className="empty-state">

                <div className="empty-state-icon">
                 <Shield size={26} />
                </div>  
             <div className="empty-state-title">Quarantine is empty</div>
            <div className="empty-state-desc">
                 Files moved to quarantine will appear here. You can restore or permanently delete them.
            </div>
          </div>

        ) : (

          <div className="table-container">

              <table className="data-table">
               <thead>

                <tr>

                  <th>Original Path</th>
                  <th>Hash</th>
                      <th>Reason</th>
                  <th>Quarantined</th>   


                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>  
                {items.map(item => (
                  <tr key={item.id}>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>

                        <FileText size={14} style={{ color: 'var(--warning)', flexShrink: 0 }} />
                        <span className="path-text" style={{ maxWidth: 320 }} title={item.originalPath}>
                             {item.originalPath}
                        </span>
                      </div>
                    </td>
                    <td> 
                      <span className="hash-code">{item.fullHash?.substring(0, 10)}…</span>

                     </td>


                      <td>
                      <span className="badge badge-warning">{item.reason || 'DUPLICATE'}</span>   
                    </td>
                       <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: 'var(--text-muted)', fontSize: '0.8rem' }}>   

                        <Clock size={12} />
                           {item.quarantinedAt

                          ? new Date(item.quarantinedAt).toLocaleString()
                          : '—'    
                        }

                      </div>
                     </td>

                    <td>
                      <div className="action-row">

                        <button
                             className="btn btn-success btn-sm"
                             onClick={() => restoreMutation.mutate(item.id)}
                          disabled={restoreMutation.isPending}
                            >
                           <RotateCcw size={12} /> 
                              Restore

                        </button>


                          <button

                          className="btn btn-danger btn-sm"
                          onClick={() => purgeMutation.mutate(item.id)}
                              disabled={purgeMutation.isPending}
                        >
                          <Trash2 size={12} />
                          Purge    
                        </button>
                      </div>   
                    </td>
                  </tr>

                ))}
              </tbody>

            </table>

          </div>

        )}
      </div>
    </div>
  );
   }

