import React, { createContext, useCallback, useContext, useState } from 'react';     

import { CheckCircle2, XCircle, AlertTriangle, Info, X } from 'lucide-react';


/* ─── Types ──────────────────────────────────────────────────────────── */
export type ToastType = 'success' | 'error' | 'warning' | 'info';

interface Toast {
  id: number;
  type: ToastType;
  title: string;
  message?: string;
}
   
interface ToastContextValue {
  toast: (type: ToastType, title: string, message?: string) => void;


  success: (title: string, message?: string) => void;
  error: (title: string, message?: string) => void;  
  warning: (title: string, message?: string) => void;
  info: (title: string, message?: string) => void;
}
 
 /* ─── Context ─────────────────────────────────────────────────────────── */
const ToastContext = createContext<ToastContextValue | null>(null);



export function useToast(): ToastContextValue {
    const ctx = useContext(ToastContext);
   if (!ctx) throw new Error('useToast must be used within <ToastProvider>');
    return ctx;

}

/* ─── Individual Toast item ──────────────────────────────────────────── */
const ICONS: Record<ToastType, React.ReactNode> = { 

  success: <CheckCircle2 size={16} />,
  error: <XCircle size={16} />,
  warning: <AlertTriangle size={16} />,
   info: <Info size={16} />,
};

const COLORS: Record<ToastType, string> = {   
    success: 'var(--success)',   
  error: 'var(--danger)',

  warning: 'var(--warning)',
  info: 'var(--accent)',
};

function ToastItem({ t, onDismiss }: { t: Toast; onDismiss: () => void }) {
  return (
      <div     
      className="toast-item"
       style={{ borderLeftColor: COLORS[t.type] }}

        role="alert"
    >


      <span className="toast-icon" style={{ color: COLORS[t.type] }}>
        {ICONS[t.type]}

       </span>
      <div className="toast-body">

        <div className="toast-title">{t.title}</div>
        {t.message && <div className="toast-message">{t.message}</div>}

      </div>
      <button className="toast-close" onClick={onDismiss} aria-label="Dismiss">
        <X size={13} />

         </button>

    </div>
  );    
}

/* ─── Provider ───────────────────────────────────────────────────────── */     
let _id = 0; 
   const DURATION = 4000;

 export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]); 


  const dismiss = useCallback((id: number) => {
        setToasts(prev => prev.filter(t => t.id !== id));
      }, []);

  const toast = useCallback((type: ToastType, title: string, message?: string) => {
       const id = ++_id;

    setToasts(prev => [...prev, { id, type, title, message }]);
    setTimeout(() => dismiss(id), DURATION);  
  }, [dismiss]);
   
  const value: ToastContextValue = {
    toast,
       success: (title, msg) => toast('success', title, msg),     
    error: (title, msg) => toast('error', title, msg),
    warning: (title, msg) => toast('warning', title, msg),
    info: (title, msg) => toast('info', title, msg),

  };
 
  return (
      <ToastContext.Provider value={value}>
      {children}     
      {/* Portal-like container — fixed, above everything */}
      <div className="toast-container" aria-live="polite">

            {toasts.map(t => (
          <ToastItem key={t.id} t={t} onDismiss={() => dismiss(t.id)} />
            ))}
      </div>     
      </ToastContext.Provider>
      );
}
