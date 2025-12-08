
import React, { useState } from 'react';
import { BrowserRouter, Routes, Route, NavLink, useLocation } from 'react-router-dom';

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import { ToastProvider } from './components/Toast';
  import DashboardOverview from './pages/Overview';

import Duplicates from './pages/Duplicates';
import Quarantine from './pages/Quarantine';
import Folders from './pages/Folders';  
   import Alerts from './pages/Alerts';
  import {

  LayoutDashboard, Copy, FolderOpen, Shield, Bell, 
  ShieldCheck, PanelLeftClose
    } from 'lucide-react';


   const queryClient = new QueryClient();


const navItems = [
  { to: '/', icon: LayoutDashboard, label: 'Overview', end: true },
  { to: '/duplicates', icon: Copy, label: 'Duplicates' },

  { to: '/folders', icon: FolderOpen, label: 'Folders' },
    { to: '/quarantine', icon: Shield, label: 'Quarantine' },

  { to: '/alerts', icon: Bell, label: 'Integrity Alerts' },
];

function AnimatedBackground() {
  return (

    <div className="animated-bg" aria-hidden="true">
      <div className="orb orb-1" />
      <div className="orb orb-2" />
    </div>
  );

}

function PageWrapper({ children }: { children: React.ReactNode }) {
  const location = useLocation();

  return (
    <div key={location.pathname} className="page-transition">  
          {children}


    </div>

  );
}


function AppShell() {  


  const [collapsed, setCollapsed] = useState(false);
  const cls = collapsed ? 'sidebar collapsed' : 'sidebar';   

  return (
    <>

        <AnimatedBackground />
      <div className="app-container">

        <aside className={cls}>
    
          {/* Logo row — always rendered, CSS hides text when collapsed */}   
          <div 
            className="sidebar-logo" 
            onClick={() => collapsed && setCollapsed(false)}
            style={{ cursor: collapsed ? 'pointer' : 'default' }}
            title={collapsed ? 'Expand sidebar' : undefined}
            aria-label={collapsed ? 'Expand sidebar' : undefined}
          >

            <div className="sidebar-logo-icon">
                  <ShieldCheck size={20} color="white" />
            </div>
            {/* Text is always in DOM — CSS transitions opacity+width */}
            <span className="sidebar-logo-text">FileGuard</span>  
            
            {!collapsed && (
              <button
                className="sidebar-collapse-btn"
                onClick={(e) => {
                  e.stopPropagation();
                  setCollapsed(true);
                }}
                title="Collapse sidebar"
                aria-label="Collapse sidebar"
              >
                <PanelLeftClose size={16} />
              </button>
            )}
          </div>
   
          {/* Section label — always in DOM */}
          <div className="sidebar-section-label">Navigation</div>
    
          <nav className="nav-links">   

            {navItems.map(({ to, icon: Icon, label, end }) => (    
              <NavLink
                    key={to}
                   to={to}

                end={end}


                  className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}

                title={label}
              >   
                <Icon size={17} className="nav-item-icon" style={{ flexShrink: 0 }} />
                {/* Label always in DOM — CSS clips it */}
                <span className="nav-label">{label}</span>


              </NavLink> 

             ))}    

          </nav>
 
          <div className="sidebar-footer">
              <div className="monitoring-status">
              <div className="monitoring-dot" />    
              <span className="sidebar-version">Monitoring Active</span>
            </div>     
              </div>  

        </aside>

        <main className="main-content">   
          <Routes>

            <Route path="/" element={<PageWrapper><DashboardOverview /></PageWrapper>} />
            <Route path="/duplicates" element={<PageWrapper><Duplicates /></PageWrapper>} />
            <Route path="/folders" element={<PageWrapper><Folders /></PageWrapper>} />
                <Route path="/quarantine" element={<PageWrapper><Quarantine /></PageWrapper>} />     
            <Route path="/alerts" element={<PageWrapper><Alerts /></PageWrapper>} />
              </Routes>

        </main>
 
      </div>
    </> 
  );
   }
    
function App() {

  return (
    <QueryClientProvider client={queryClient}>
      <ToastProvider>

        <BrowserRouter>  
            <AppShell />
        </BrowserRouter>
      </ToastProvider>
        </QueryClientProvider>
   );

   }


export default App;
