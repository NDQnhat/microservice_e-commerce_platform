import React, { useState } from 'react';
import { Copy, Check, ChevronDown, ChevronRight } from 'lucide-react';

interface JsonViewerProps {
  data: unknown;
  title?: string;
  defaultExpanded?: boolean;
}

export const JsonViewer: React.FC<JsonViewerProps> = ({
  data,
  title = 'Raw JSON Payload',
  defaultExpanded = true,
}) => {
  const [expanded, setExpanded] = useState(defaultExpanded);
  const [copied, setCopied] = useState(false);

  let formatted = '';
  try {
    if (typeof data === 'string') {
      try {
        formatted = JSON.stringify(JSON.parse(data), null, 2);
      } catch {
        formatted = data;
      }
    } else {
      formatted = JSON.stringify(data, null, 2);
    }
  } catch {
    formatted = String(data);
  }

  const handleCopy = () => {
    navigator.clipboard.writeText(formatted);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="border border-slate-800 rounded-lg overflow-hidden bg-slate-950 font-mono text-xs">
      <div className="flex items-center justify-between px-3 py-2 bg-slate-900/90 border-b border-slate-800/80">
        <button
          onClick={() => setExpanded(!expanded)}
          className="flex items-center gap-1.5 text-slate-300 hover:text-white font-semibold"
        >
          {expanded ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
          <span>{title}</span>
        </button>
        <button
          onClick={handleCopy}
          className="flex items-center gap-1 text-slate-400 hover:text-white px-2 py-0.5 rounded hover:bg-slate-800 transition"
          title="Copy JSON to clipboard"
        >
          {copied ? (
            <>
              <Check className="h-3.5 w-3.5 text-emerald-400" />
              <span className="text-emerald-400 text-[11px]">Copied</span>
            </>
          ) : (
            <>
              <Copy className="h-3.5 w-3.5" />
              <span className="text-[11px]">Copy</span>
            </>
          )}
        </button>
      </div>

      {expanded && (
        <div className="p-3 max-h-72 overflow-auto text-emerald-400/90 leading-relaxed whitespace-pre font-mono selection:bg-indigo-600/40">
          {formatted || '<empty>'}
        </div>
      )}
    </div>
  );
};
