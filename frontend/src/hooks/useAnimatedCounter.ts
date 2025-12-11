
import { useEffect, useRef, useState } from 'react';

/**   
 * Animates a number from 0 to `target` over `duration` ms     
 * using an ease-out curve. Returns the current display value.
 */
 export function useAnimatedCounter(

  target: number,   


  duration = 1200,
  enabled = true

): number {
  const [display, setDisplay] = useState(0);    
  const startRef = useRef<number | null>(null);

  const rafRef = useRef<number>(0);
    
  useEffect(() => {     
    if (!enabled || isNaN(target)) return;
    
    startRef.current = null;


    const step = (timestamp: number) => { 

      if (startRef.current === null) startRef.current = timestamp;   

          const elapsed = timestamp - startRef.current;
      const progress = Math.min(elapsed / duration, 1);    
      // ease-out cubic  
      const eased = 1 - Math.pow(1 - progress, 3);
      setDisplay(Math.round(eased * target));  

      if (progress < 1) rafRef.current = requestAnimationFrame(step);
      };

    rafRef.current = requestAnimationFrame(step);   

    return () => cancelAnimationFrame(rafRef.current);
  }, [target, duration, enabled]);


    return display;
}
