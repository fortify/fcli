(function() {
  'use strict';
  
  // Extract the current page path relative to the version root
  function getCurrentPagePath() {
    const path = window.location.pathname;
    const baseurl = '/fcli';
    
    // Remove baseurl prefix
    let relativePath = path.startsWith(baseurl) ? path.substring(baseurl.length) : path;
    
    // Remove leading slash
    if (relativePath.startsWith('/')) {
      relativePath = relativePath.substring(1);
    }
    
    // Remove version prefix (v3.14.3/, dev_v3.x/, etc.)
    const versionMatch = relativePath.match(/^(v[^\/]+|dev_[^\/]+)\/(.*)/);
    if (versionMatch) {
      return versionMatch[2] || 'index.html';
    }
    
    // If we're at the version root, return index.html
    return 'index.html';
  }
  
  // Check if a URL exists using HEAD request with timeout
  function checkPageExists(url, timeout) {
    return new Promise((resolve) => {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => {
        controller.abort();
        console.log('[Version Nav] Timeout checking:', url);
      }, timeout);
      
      fetch(url, { 
        method: 'HEAD',
        signal: controller.signal
      })
        .then(response => {
          clearTimeout(timeoutId);
          console.log('[Version Nav] HEAD request result:', url, '→', response.status);
          resolve(response.ok);
        })
        .catch((error) => {
          clearTimeout(timeoutId);
          console.log('[Version Nav] HEAD request failed:', url, '→', error.message);
          resolve(false);
        });
    });
  }
  
  // Navigate to the same page in the target version if it exists
  async function navigateToVersion(event, targetVersion) {
    event.preventDefault();
    
    const baseurl = '/fcli';
    const currentPagePath = getCurrentPagePath();
    console.log('[Version Nav] Current page path:', currentPagePath);
    console.log('[Version Nav] Target version:', targetVersion);
    
    const targetUrl = `${baseurl}/${targetVersion}/${currentPagePath}`;
    const fallbackUrl = `${baseurl}/${targetVersion}/index.html`;
    
    console.log('[Version Nav] Target URL:', targetUrl);
    console.log('[Version Nav] Fallback URL:', fallbackUrl);
    
    // If already on index.html, just navigate to target version index
    if (currentPagePath === 'index.html' || currentPagePath === '') {
      console.log('[Version Nav] On index, navigating to:', fallbackUrl);
      window.location.href = fallbackUrl;
      return;
    }
    
    // Check if the target page exists
    console.log('[Version Nav] Checking if page exists...');
    const exists = await checkPageExists(targetUrl, 500);
    
    if (exists) {
      console.log('[Version Nav] Page exists, navigating to:', targetUrl);
      window.location.href = targetUrl;
    } else {
      console.log('[Version Nav] Page does not exist, navigating to:', fallbackUrl);
      window.location.href = fallbackUrl;
    }
  }
  
  // Set up event delegation on dropdown content
  function initVersionNavigation() {
    const dropdownContent = document.querySelector('.dropdown-content');
    if (!dropdownContent) {
      console.log('[Version Nav] No dropdown-content found');
      return;
    }
    
    console.log('[Version Nav] Initialized on dropdown-content');
    
    dropdownContent.addEventListener('click', function(event) {
      const link = event.target.closest('a.version-link');
      if (!link) {
        return;
      }
      
      console.log('[Version Nav] Version link clicked:', link.href);
      
      // Extract version from href - match the last path segment
      const href = link.getAttribute('href');
      const versionMatch = href.match(/\/(v[^\/]+|dev_[^\/]+)\/?$/);
      if (versionMatch) {
        const targetVersion = versionMatch[1];
        navigateToVersion(event, targetVersion);
      } else {
        console.log('[Version Nav] Could not extract version from href:', href);
      }
    });
  }
  
  // Initialize when DOM is ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initVersionNavigation);
  } else {
    initVersionNavigation();
  }
})();
