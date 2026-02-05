/**
 * Code Copy Button for FCLI Documentation
 * Adds copy-to-clipboard functionality to code blocks
 */

(function() {
  'use strict';

  /**
   * Add copy buttons to all code blocks
   */
  function addCopyButtons() {
    const codeBlocks = document.querySelectorAll('.listingblock > .content > pre, .literalblock > .content > pre');
    
    codeBlocks.forEach(function(codeBlock) {
      const container = codeBlock.closest('.listingblock, .literalblock');
      if (!container || container.querySelector('.copy-button')) {
        return; // Skip if no container or button already exists
      }
      
      const button = document.createElement('button');
      button.className = 'copy-button';
      button.textContent = 'Copy';
      button.setAttribute('aria-label', 'Copy code to clipboard');
      
      button.addEventListener('click', function() {
        const code = codeBlock.textContent;
        
        if (navigator.clipboard && navigator.clipboard.writeText) {
          // Use modern clipboard API
          navigator.clipboard.writeText(code).then(function() {
            showCopySuccess(button);
          }).catch(function(err) {
            console.error('Failed to copy:', err);
            fallbackCopy(code, button);
          });
        } else {
          // Fallback for older browsers
          fallbackCopy(code, button);
        }
      });
      
      // Ensure container has relative positioning
      container.style.position = 'relative';
      container.insertBefore(button, container.firstChild);
    });
  }
  
  /**
   * Show success feedback when code is copied
   */
  function showCopySuccess(button) {
    const originalText = button.textContent;
    button.textContent = 'Copied!';
    button.classList.add('copied');
    
    setTimeout(function() {
      button.textContent = originalText;
      button.classList.remove('copied');
    }, 2000);
  }
  
  /**
   * Fallback copy method for browsers without clipboard API
   */
  function fallbackCopy(text, button) {
    const textArea = document.createElement('textarea');
    textArea.value = text;
    textArea.style.position = 'fixed';
    textArea.style.left = '-999999px';
    textArea.style.top = '-999999px';
    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();
    
    try {
      document.execCommand('copy');
      showCopySuccess(button);
    } catch (err) {
      console.error('Fallback copy failed:', err);
    }
    
    document.body.removeChild(textArea);
  }

  // Initialize copy buttons after DOM is ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', addCopyButtons);
  } else {
    addCopyButtons();
  }

})();
