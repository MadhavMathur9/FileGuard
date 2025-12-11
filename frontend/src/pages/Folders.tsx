import React, { useState, useRef } from 'react';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

import { FolderOpen, Plus, Trash2, ScanLine, CheckCircle2, XCircle, Upload } from 'lucide-react';  
import { useToast } from '../components/Toast';
import { TableRowSkeleton } from '../components/Skeleton';
    

interface WatchedFolder {
  id: number;
     path: string; 

  recursive: boolean;
    mode: string;
  createdAt: string;

}   
   
  export default function Folders() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [newPath, setNewPath] = useState('');
      const [dragOver, setDragOver] = useState(false); 
  const dropRef = useRef<HTMLDivElement>(null);

  const { data: folders, isLoading } = useQuery<WatchedFolder[]>({
    queryKey: ['folders'],
    queryFn: async () => {
      const res = await fetch('/api/v1/folders');

         if (!res.ok) throw new Error('Network response was not ok');

      return res.json();

    },
  });
   
     const addFolderMutation = useMutation({     

      mutationFn: async (path: string) => {

      const res = await fetch('/api/v1/folders', {
           method: 'POST',


          headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ path, recursive: true, mode: 'ACTIVE' }),

      });  
      if (!res.ok) throw new Error('Failed to add folder');     
      return res.json();
    },
    onSuccess: (_, path) => {
      queryClient.invalidateQueries({ queryKey: ['folders'] });
          setNewPath('');
      toast.success('Folder added', `Now monitoring: ${path}`);
    },

    onError: () => {
       toast.error('Failed to add folder', 'Check the path exists and try again.');
    },

  });     

  const removeFolderMutation = useMutation({
    mutationFn: async (id: number) => {
          const res = await fetch(`/api/v1/folders/${id}`, { method: 'DELETE' });
        if (!res.ok) throw new Error('Failed to remove folder');
        },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['folders'] });
      toast.success('Folder removed', 'No longer monitoring this directory.');
    },
    onError: () => toast.error('Failed to remove folder'),

  });

  const scanFolderMutation = useMutation({
    mutationFn: async (id: number) => {
      const res = await fetch(`/api/v1/folders/${id}/scan?deep=true`, { method: 'POST' });
        if (!res.ok) throw new Error('Failed to scan folder');

      return id;   
     },     
    onSuccess: () => {

      toast.success('Scan started', 'Deep scan queued — results will appear shortly.');
       },
    onError: () => toast.error('Scan failed', 'Could not start folder scan.'),
  });     

  const handleAdd = (path: string) => {
    const trimmed = path.trim();

    if (!trimmed) return;

        addFolderMutation.mutate(trimmed);
  };


    // Drag-and-drop handlers (accept dragged text / path paste)
  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(true);
  };

  const handleDragLeave = () => setDragOver(false);
  const handleDrop = (e: React.DragEvent) => {
      e.preventDefault();
    setDragOver(false);     

       const text = e.dataTransfer.getData('text/plain')?.trim();
    if (text) {    
          setNewPath(text);    
          handleAdd(text);
     }
  };

   const modeBadge = (mode: string) => {
    const map: Record<string, string> = { 

      ACTIVE: 'badge-blue',
          DEDUP: 'badge-warning',
      INTEGRITY: 'badge-purple',
        BOTH: 'badge-success',  
    };

    return map[mode] || 'badge-blue';
  };

  return (
    <div>

      <div className="page-header">
        <h1 className="page-title">Monitored Folders</h1>

        <p className="page-subtitle">Manage which directories FileGuard actively watches</p>

      </div>

      {/* Add Folder Card */}
       <div className="glass-panel" style={{ marginBottom: 24 }}>  
        <div className="section-title" style={{ marginBottom: 16, display: 'flex', alignItems: 'center', gap: 8 }}>
          <Plus size={16} style={{ color: 'var(--accent)' }} />

          Add New Folder
        </div>  
    
        {/* Text input row */}     
        <div className="input-row" style={{ marginBottom: 16 }}>
          <input
             className="input-field"
            type="text"    

            value={newPath}
            onChange={e => setNewPath(e.target.value)}
            placeholder="e.g. C:\Users\you\Documents"
            onKeyDown={e => e.key === 'Enter' && handleAdd(newPath)}

          />

          <button
            className="btn btn-primary"
            style={{ flexShrink: 0 }}   

             onClick={() => handleAdd(newPath)}
              disabled={!newPath || addFolderMutation.isPending}

          >
            <Plus size={15} />
            {addFolderMutation.isPending ? 'Adding…' : 'Add Folder'}

          </button>

          </div>
    
        {/* Drag-and-drop zone */}
        <div
          ref={dropRef}
              className={`drop-zone${dragOver ? ' drag-over' : ''}`}   
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}     
          onDrop={handleDrop}

          onClick={() => {     

             const p = window.prompt('Paste or type a folder path:');   
            if (p) handleAdd(p);
          }}


        >
          <div className="drop-zone-icon">

            <Upload size={18} />
          </div>  
          <div style={{ fontWeight: 600, marginBottom: 4, color: dragOver ? 'var(--accent)' : 'var(--text-secondary)' }}>
            Drag a folder path here
              </div> 
          <div style={{ fontSize: '0.8rem' }}>
            Or click to paste a path manually
          </div>
        </div>
      </div>

       {/* Folders List */}
        <div className="glass-panel" style={{ padding: 0, overflow: 'hidden' }}>

        {isLoading ? (
              <table className="data-table" style={{ width: '100%' }}>
            <tbody><TableRowSkeleton cols={5} rows={3} /></tbody>
          </table>
        ) : !folders || folders.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon">
              <FolderOpen size={26} />
            </div>

            <div className="empty-state-title">No folders monitored</div>


            <div className="empty-state-desc">
              Add a folder path above to start monitoring it for duplicates and integrity changes.  

              </div>

          </div>
         ) : (  

          <div className="table-container">

            <table className="data-table">
                  <thead>
                <tr>
                  <th>Path</th> 

                  <th>Mode</th>
                  <th>Recursive</th>    
                  <th>Added</th>
                  <th>Actions</th>
                </tr>
                  </thead>
              <tbody>
                {folders.map(folder => (


                  <tr key={folder.id}>
                    <td>

                      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}> 
                        <FolderOpen size={14} style={{ color: 'var(--accent)', flexShrink: 0 }} />


                        <span className="path-text" style={{ maxWidth: 360 }} title={folder.path}>
                          {folder.path}
                            </span>
                      </div>
                    </td>    
                    <td>
                      <span className={`badge ${modeBadge(folder.mode)}`}>{folder.mode}</span>
                    </td>
                    <td>
                      {folder.recursive

                          ? <span className="badge badge-success"><CheckCircle2 size={9} /> Yes</span>
                        : <span className="badge badge-danger"><XCircle size={9} /> No</span>
                      }

                    </td>
                       <td style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>
                      {folder.createdAt ? new Date(folder.createdAt).toLocaleDateString() : '—'}
                      </td>
                    <td>
                          <div className="action-row">
                         <button
                          className="btn btn-primary btn-sm"
                          onClick={() => scanFolderMutation.mutate(folder.id)}
                          disabled={scanFolderMutation.isPending}
                        >
                          <ScanLine size={12} /> Scan   
                        </button>
                        <button
                          className="btn btn-danger btn-sm"
                          onClick={() => removeFolderMutation.mutate(folder.id)}
                          disabled={removeFolderMutation.isPending} 

                          >
                          <Trash2 size={12} /> Remove
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
