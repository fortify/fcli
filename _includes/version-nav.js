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
      const timeoutId = setTimeout(() => controller.abort(), timeout);
      
      fetch(url, { 
        method: 'HEAD',
        signal: controller.signal
      })
        .then(response => {
          clearTimeout(timeoutId);
          resolve(response.ok);
        })
        .catch(() => {
          clearTimeout(timeoutId);
          resolve(false);
        });
    });
  }
  
  // Navigate to the same page in the target version if it exists
  async function navigateToVersion(event, targetVersion) {
    event.preventDefault();
    
    const baseurl = '/fcli';
    const currentPagePath = getCurrentPagePath();
    const targetUrl = `${baseurl}/${targetVersion}/${currentPagePath}`;
    const fallbackUrl = `${baseurl}/${targetVersion}/index.html`;
    
    // If already on index.html, just navigate to target version index
    if (currentPagePath === 'index.html' || currentPagePath === '') {
      window.location.href = fallbackUrl;
      return;
    }
    
    // Check if the target page exists
    const exists = await checkPageExists(targetUrl, 500);
    
    if (exists) {
      window.location.href = targetUrl;
    } else {
      window.location.href = fallbackUrl;
    }
  }
  
  // Set up event delegation on dropdown content
  function initVersionNavigation() {
    const dropdownContent = document.querySelector('.dropdown-content');
    if (!dropdownContent) {
      return;
    }
    
    dropdownContent.addEventListener('click', function(event) {
      const link = event.target.closest('a.version-link');
      if (!link) {
        return;
      }
      
      // Extract version from href
      const href = link.getAttribute('href');
      const versionMatch = href.match(/\/fcli\/(v[^\/]+|dev_[^\/]+)$/);
      if (versionMatch) {
        const targetVersion = versionMatch[1];
        navigateToVersion(event, targetVersion);
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
