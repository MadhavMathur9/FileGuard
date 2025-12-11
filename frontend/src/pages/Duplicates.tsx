    import React, { useMemo, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
    Copy, Shield, Trash2, ChevronDown, ChevronUp,

  FileText, Search, ArrowUpDown, ArrowUp, ArrowDown,

   } from 'lucide-react';
import { useToast } from '../components/Toast';
import { TableRowSkeleton } from '../components/Skeleton';

interface FileEntry {
  id: number;
  path: string;
   size: number;

  mtime: string;
}

interface DuplicateGroup {
  id: number;
  fullHash: string;

  memberCount: number;


  wastedBytes: number;
    originalFileId: number;


  files: FileEntry[];
}     

   interface Page<T> {
      content: T[];    
  totalElements: number;
  totalPages: number;
    size: number;
  number: number;
}

const formatBytes = (bytes: number) => {
  if (!bytes) return '0 B'; 
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];

  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];

};

  
 type SortKey = 'memberCount' | 'wastedBytes' | 'fullHash';  
  type SortDir = 'asc' | 'desc';

   

    function SortIcon({ col, active, dir }: { col: string; active: boolean; dir: SortDir }) {     
     if (!active) return <ArrowUpDown size={11} style={{ marginLeft: 4, opacity: 0.4 }} />;
  return dir === 'asc'
       ? <ArrowUp size={11} style={{ marginLeft: 4, color: 'var(--accent)' }} />   

    : <ArrowDown size={11} style={{ marginLeft: 4, color: 'var(--accent)' }} />;     

} 

function FileList({ files, originalId }: { files: FileEntry[]; originalId: number }) {   
  return (
    <div style={{ padding: '12px 16px 16px', background: 'rgba(0,0,0,0.2)', borderTop: '1px solid var(--glass-border)' }}>
      <div style={{ fontSize: '0.72rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.08em', color: 'var(--text-muted)', marginBottom: 10 }}>   
        Files in this group
      </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>     

           {files.map(f => (
            <div key={f.id} style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '8px 12px', background: 'rgba(255,255,255,0.02)', borderRadius: 8, border: '1px solid var(--border-subtle)', transition: 'background 0.15s' }}>
            <FileText size={13} style={{ color: f.id === originalId ? 'var(--success)' : 'var(--text-muted)', flexShrink: 0 }} />
            <span className="path-text" style={{ flex: 1 }} title={f.path}>{f.path}</span>

                {f.id === originalId && <span className="badge badge-success">Original</span>}

            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', flexShrink: 0 }}>{formatBytes(f.size)}</span>
          </div>
        ))}
       </div>
    </div>

  );
}


export default function Duplicates() {   
  const queryClient = useQueryClient();
  const toast = useToast(); 
   const [expandedId, setExpandedId] = useState<number | null>(null);


  const [search, setSearch] = useState('');
  const [sortKey, setSortKey] = useState<SortKey>('wastedBytes');
    const [sortDir, setSortDir] = useState<SortDir>('desc');

  const { data, isLoading } = useQuery<Page<DuplicateGroup>>({
    queryKey: ['duplicates'],

    queryFn: async () => {

        const res = await fetch('/api/v1/duplicates');
      if (!res.ok) throw new Error('Failed to load duplicates');

      return res.json();
    },
  });


  const applyActionMutation = useMutation({
    mutationFn: async ({ groupId, action, dryRun }: { groupId: number; action: string; dryRun: boolean }) => {
      const res = await fetch(`/api/v1/duplicates/${groupId}/apply`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },

        body: JSON.stringify({ action, dryRun }),
      });
      if (!res.ok) throw new Error('Failed to apply action');
       return res.json();
     },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['duplicates'] });
      queryClient.invalidateQueries({ queryKey: ['stats'] });

      queryClient.invalidateQueries({ queryKey: ['quarantine'] });

      if (variables.action === 'DELETE') {
        toast.success('Duplicates deleted', 'Copies removed, original kept.');

      } else {
        toast.success('Moved to quarantine', 'Files safely quarantined.');

      }
    },     

    onError: (_, variables) => {
         toast.error(`Failed to ${variables.action.toLowerCase()}`, 'Check backend logs for details.');
    },   
  });

   const handleSort = (key: SortKey) => {
       if (sortKey === key) setSortDir(d => d === 'asc' ? 'desc' : 'asc');
     else { setSortKey(key); setSortDir('desc'); }
      };
    
   const duplicates = useMemo(() => {
    let list = data?.content || [];

    // Filter by search
     if (search.trim()) {

      const q = search.toLowerCase();
          list = list.filter(g =>
         g.fullHash.toLowerCase().includes(q) ||

        g.files?.some(f => f.path.toLowerCase().includes(q))

      );
      }   
    // Sort
    return [...list].sort((a, b) => {
      const aVal = a[sortKey];

       const bVal = b[sortKey];

      const cmp = typeof aVal === 'string'


          ? aVal.localeCompare(bVal as string)
           : (aVal as number) - (bVal as number);  
          return sortDir === 'asc' ? cmp : -cmp;    
     });
  }, [data, search, sortKey, sortDir]);

  const thProps = (key: SortKey) => ({     
    className: `th-sortable${sortKey === key ? ' active' : ''}`,
    onClick: () => handleSort(key),
    });    

   return (    
    <div>   
      <div className="page-header">
        <h1 className="page-title">Duplicates</h1>  
        <p className="page-subtitle">

           {isLoading ? 'Loading…' : `${data?.totalElements ?? 0} duplicate group${data?.totalElements !== 1 ? 's' : ''} detected`}
        </p>  

      </div>

      {/* Search + sort toolbar */}

      <div style={{ display: 'flex', gap: 12, marginBottom: 20, alignItems: 'center' }}>
        <div className="search-bar" style={{ flex: 1 }}>
          <Search size={15} style={{ color: 'var(--text-muted)', flexShrink: 0 }} />
          <input
               placeholder="Search by hash or file path…"
            value={search}    
              onChange={e => setSearch(e.target.value)}
          />     
        </div>

           <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>  

          {duplicates.length} result{duplicates.length !== 1 ? 's' : ''}
        </span>
      </div>

          {isLoading ? (
        <div className="glass-panel" style={{ padding: 0, overflow: 'hidden' }}>
          <table className="data-table" style={{ width: '100%' }}>
            <tbody><TableRowSkeleton cols={4} rows={5} /></tbody>

          </table>

        </div>    
      ) : duplicates.length === 0 ? (     

           <div className="glass-panel">
          <div className="empty-state">

            <div className="empty-state-icon">
              <Copy size={26} />
            </div>

            <div className="empty-state-title">
              {search ? 'No matches found' : 'No duplicates found'}
            </div>
            <div className="empty-state-desc">
              {search ? `No groups match "${search}". Try a different search.` : 'Your monitored folders are clean.'}
            </div>
          </div>
        </div>
       ) : (  

        <div className="glass-panel" style={{ padding: 0, overflow: 'hidden' }}>
              {/* Sortable table header */}
            <div style={{ padding: '12px 20px', borderBottom: '1px solid var(--glass-border)', background: 'rgba(217,79,30,0.04)' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.72rem', letterSpacing: '0.08em', textTransform: 'uppercase' }}>

                <thead>

                   <tr>
                  <th style={{ textAlign: 'left', color: 'var(--text-muted)', fontWeight: 700, width: '40%' }}>File / Hash</th>
                     <th {...thProps('memberCount')} style={{ textAlign: 'left', color: sortKey === 'memberCount' ? 'var(--accent)' : 'var(--text-muted)', fontWeight: 700, width: '15%' }}>

                      Copies <SortIcon col="memberCount" active={sortKey === 'memberCount'} dir={sortDir} />
                  </th>
                    <th {...thProps('wastedBytes')} style={{ textAlign: 'left', color: sortKey === 'wastedBytes' ? 'var(--accent)' : 'var(--text-muted)', fontWeight: 700, width: '20%' }}>
                    Wasted <SortIcon col="wastedBytes" active={sortKey === 'wastedBytes'} dir={sortDir} />
                  </th>
                    <th style={{ textAlign: 'right', color: 'var(--text-muted)', fontWeight: 700 }}>Actions</th>

                </tr>
              </thead>
            </table>
          </div>


           {/* Group rows */}
             <div style={{ display: 'flex', flexDirection: 'column' }}>
             {duplicates.map(group => {
              const originalFile = group.files?.find(f => f.id === group.originalFileId);

              const isExpanded = expandedId === group.id;
              const isPending = applyActionMutation.isPending;
 
              return (

                <div key={group.id} style={{ borderBottom: '1px solid rgba(255,255,255,0.03)' }}>

                  <div style={{ display: 'flex', alignItems: 'center', gap: 16, padding: '16px 20px', transition: 'background 0.14s' }}

                    onMouseEnter={e => (e.currentTarget.style.background = 'rgba(217,79,30,0.04)')}   
                    onMouseLeave={e => (e.currentTarget.style.background = '')}

                    >
                    <div style={{ flexShrink: 0 }}>
                        <span className="hash-code">{group.fullHash.substring(0, 10)}…</span>

                      </div>  
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div className="path-text" title={originalFile?.path}>
                        {originalFile?.path || <span className="text-muted">Unknown origin</span>}
                      </div>
                       <div style={{ display: 'flex', gap: 8, marginTop: 6 }}>
                        <span className="badge badge-warning"><Copy size={9} />{group.memberCount} copies</span>
                         <span className="badge badge-danger"><Trash2 size={9} />{formatBytes(group.wastedBytes)} wasted</span>   
                       </div>
                    </div>   
                      <div className="action-row" style={{ flexShrink: 0 }}>
                      <button className="btn btn-purple btn-sm" disabled={isPending}  
                          onClick={() => applyActionMutation.mutate({ groupId: group.id, action: 'QUARANTINE', dryRun: false })}>
                        <Shield size={13} /> Quarantine

                      </button>

                          <button className="btn btn-danger btn-sm" disabled={isPending}
                          onClick={() => applyActionMutation.mutate({ groupId: group.id, action: 'DELETE', dryRun: false })}>
                          <Trash2 size={13} /> Delete

                      </button>

                      <button className="btn btn-ghost btn-sm" 

                          onClick={() => setExpandedId(isExpanded ? null : group.id)}>
                        {isExpanded ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
                         </button>
                        </div>     
                   </div>

                    {isExpanded && group.files && (
                    <FileList files={group.files} originalId={group.originalFileId} />    
                    )} 

                </div>
                );
               })}

          </div>
        </div>
      )}
    </div>
      );

} 
