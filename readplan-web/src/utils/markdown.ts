export const renderMarkdown = (markdown: string): string => {
  if (!markdown) {
    return '';
  }

  // 1. Escape HTML to prevent XSS
  let html = markdown
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');

  // 2. Code blocks (```code```)
  html = html.replace(/```([\s\S]+?)```/g, (_, code) => {
    return `<pre style="background: rgb(244 245 247); padding: 12px 16px; border-radius: 12px; font-family: monospace; overflow-x: auto; border: 1px solid var(--page-border, rgba(0, 0, 0, 0.08)); margin: 12px 0; font-size: 13px; line-height: 1.5; white-space: pre;"><code>${code.trim()}</code></pre>`;
  });

  // 3. Inline code (`code`)
  html = html.replace(/`([^`\n]+?)`/g, '<code style="background: rgba(122, 77, 17, 0.08); color: #7a4d11; padding: 2px 6px; border-radius: 6px; font-family: monospace; font-size: 0.9em;">$1</code>');

  // 4. Bold text (**text**)
  html = html.replace(/\*\*([^*]+?)\*\*/g, '<strong>$1</strong>');

  // 5. Headers (# to ######)
  html = html.replace(/^(#{1,6})\s+(.+)$/gm, (_, hashes, content) => {
    const level = hashes.length;
    const sizes = [28, 24, 20, 18, 16, 14];
    const size = sizes[level - 1] || 16;
    return `<h${level} style="margin: 16px 0 8px; font-size: ${size}px; font-weight: 700; color: #1f2937; line-height: 1.3;">${content}</h${level}>`;
  });

  // 6. Lists (- item)
  html = html.replace(/^\s*-\s+(.+)$/gm, '<li>$1</li>');
  html = html.replace(/(<li>[\s\S]+?<\/li>)/g, (match) => {
    return `<ul style="margin: 8px 0; padding-left: 20px; list-style-type: disc; display: grid; gap: 4px;">${match}</ul>`;
  });
  html = html.replace(/<\/ul>\s*<ul style="margin: 8px 0; padding-left: 20px; list-style-type: disc; display: grid; gap: 4px;">/g, '\n');

  // 7. Line breaks (handle newlines, skip replacing inside <pre>)
  const parts = html.split(/(<pre[\s\S]+?<\/pre>)/g);
  for (let i = 0; i < parts.length; i++) {
    if (!parts[i].startsWith('<pre')) {
      parts[i] = parts[i].replace(/\n/g, '<br/>');
    }
  }
  html = parts.join('');

  return html;
};
