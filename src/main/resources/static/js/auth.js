// Create stars
function createStars() {
    const starsContainer = document.getElementById('stars');
    const starCount = 200;
    
    for (let i = 0; i < starCount; i++) {
      const star = document.createElement('div');
      star.className = 'star';
      
      // Random size between 1px and 3px
      const size = Math.random() * 2 + 1;
      star.style.width = `${size}px`;
      star.style.height = `${size}px`;
      
      // Random position
      const posX = Math.random() * 100;
      const posY = Math.random() * 100;
      star.style.left = `${posX}%`;
      star.style.top = `${posY}%`;
      
      // Random animation duration between 3s and 8s
      const duration = Math.random() * 5 + 3;
      star.style.animationDuration = `${duration}s`;
      
      // Random animation delay
      const delay = Math.random() * 8;
      star.style.animationDelay = `${delay}s`;
      
      starsContainer.appendChild(star);
    }
  }
  
  // Create floating orbs
  function createOrbs() {
    const orbsContainer = document.getElementById('orbs-container');
    const orbCount = 8;
    const colors = [
      'rgba(52, 152, 219, 0.4)', // Blue
      'rgba(155, 89, 182, 0.3)', // Purple
      'rgba(52, 152, 219, 0.25)', // Light blue
      'rgba(46, 204, 113, 0.2)'  // Green
    ];
    
    for (let i = 0; i < orbCount; i++) {
      const orb = document.createElement('div');
      orb.className = 'orb';
      
      // Random size between 100px and 300px
      const size = Math.random() * 200 + 100;
      orb.style.width = `${size}px`;
      orb.style.height = `${size}px`;
      
      // Random position
      const posX = Math.random() * 100;
      const posY = Math.random() * 100;
      orb.style.left = `${posX}%`;
      orb.style.top = `${posY}%`;
      
      // Random color
      const color = colors[Math.floor(Math.random() * colors.length)];
      orb.style.background = color;
      
      // Set initial transform
      orb.dataset.x = posX;
      orb.dataset.y = posY;
      orb.dataset.size = size;
      
      orbsContainer.appendChild(orb);
    }
  }
  
  // Animate orbs
  function animateOrbs() {
    const orbs = document.querySelectorAll('.orb');
    
    orbs.forEach(orb => {
      const x = parseFloat(orb.dataset.x);
      const y = parseFloat(orb.dataset.y);
      
      // Create movement effect
      const offsetX = (Math.random() - 0.5) * 15;
      const offsetY = (Math.random() - 0.5) * 15;
      const sizeChange = (Math.random() - 0.5) * 30;
      
      orb.style.transform = `translate(${offsetX}px, ${offsetY}px) scale(${1 + sizeChange/100})`;
      
      setTimeout(() => {
        orb.style.transform = 'translate(0px, 0px) scale(1)';
      }, 5000);
    });
    
    setTimeout(animateOrbs, 8000);
  }
  
  // Create digital particles
  function createDigitalParticles() {
    const particlesContainer = document.getElementById('digital-particles');
    const particleCount = 25;
    
    for (let i = 0; i < particleCount; i++) {
      const particle = document.createElement('div');
      particle.className = 'd-particle';
      
      // Random size between 2px and 10px
      const size = Math.random() * 8 + 2;
      particle.style.width = `${size}px`;
      particle.style.height = `${size}px`;
      
      // Random position
      const posX = Math.random() * 100;
      particle.style.left = `${posX}%`;
      
      // Random animation duration between 20s and 40s
      const duration = Math.random() * 20 + 20;
      particle.style.animationDuration = `${duration}s`;
      
      // Random delay
      const delay = Math.random() * 20;
      particle.style.animationDelay = `${delay}s`;
      
      // Color gradient
      const opacity = Math.random() * 0.3 + 0.2;
      particle.style.background = `rgba(52, 152, 219, ${opacity})`;
      
      particlesContainer.appendChild(particle);
    }
  }
  
  // Create glowing lines
  function createGlowLines() {
    const linesContainer = document.getElementById('glow-lines');
    const lineCount = 15;
    
    for (let i = 0; i < lineCount; i++) {
      const line = document.createElement('div');
      line.className = 'glow-line';
      
      // Random position
      const posY = Math.random() * 100;
      line.style.top = `${posY}%`;
      
      // Random animation duration between 8s and 20s
      const duration = Math.random() * 12 + 8;
      line.style.animationDuration = `${duration}s`;
      
      // Random delay
      const delay = Math.random() * 10;
      line.style.animationDelay = `${delay}s`;
      
      linesContainer.appendChild(line);
    }
  }
  
  // Create network nodes and connections
  function createNetwork() {
    const networkContainer = document.getElementById('network');
    const nodeCount = 30;
    const nodes = [];
    
    // Create nodes
    for (let i = 0; i < nodeCount; i++) {
      const node = document.createElement('div');
      node.className = 'node';
      
      // Random position
      const posX = Math.random() * 100;
      const posY = Math.random() * 100;
      node.style.left = `${posX}%`;
      node.style.top = `${posY}%`;
      
      nodes.push({ element: node, x: posX, y: posY });
      networkContainer.appendChild(node);
    }
  }
  
  // Create pulse effect
  function createPulseEffect() {
    const pulseContainer = document.getElementById('pulse');
    const pulseCount = 4;
    
    for (let i = 0; i < pulseCount; i++) {
      const pulse = document.createElement('div');
      pulse.className = 'pulse-ring';
      
      // Random position
      const posX = Math.random() * 80 + 10;
      const posY = Math.random() * 80 + 10;
      pulse.style.left = `${posX}%`;
      pulse.style.top = `${posY}%`;
      
      // Random size
      const size = Math.random() * 200 + 100;
      pulse.style.width = `${size}px`;
      pulse.style.height = `${size}px`;
      
      // Animation
      const duration = Math.random() * 8 + 6;
      pulse.style.animationDuration = `${duration}s`;
      
      // Random delay
      const delay = Math.random() * 10;
      pulse.style.animationDelay = `${delay}s`;
      
      pulseContainer.appendChild(pulse);
    }
  }

  // Initialize all effects
  document.addEventListener('DOMContentLoaded', function() {
    createStars();
    createOrbs();
    setTimeout(animateOrbs, 1000);
    createDigitalParticles();
    createGlowLines();
    createNetwork();
    createPulseEffect();
    
    // Interactive background movement based on mouse position
    document.addEventListener('mousemove', function(e) {
      const x = e.clientX / window.innerWidth;
      const y = e.clientY / window.innerHeight;
      
      // Move orbs slightly with mouse
      const orbs = document.querySelectorAll('.orb');
      orbs.forEach(orb => {
        const moveX = (x - 0.5) * 20;
        const moveY = (y - 0.5) * 20;
        orb.style.transform = `translate(${moveX}px, ${moveY}px)`;
      });
      
      // Move stars slightly
      const stars = document.getElementById('stars');
      stars.style.transform = `translate(${(x - 0.5) * 10}px, ${(y - 0.5) * 10}px)`;
    });
  });