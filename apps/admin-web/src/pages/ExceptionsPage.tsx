import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';
import { ExceptionRecord, ExceptionRecordStatus } from '@/types';
import { AlertTriangle, CheckCircle, ShieldAlert, UserCheck } from 'lucide-react';

interface PaginatedExceptions {
  content: ExceptionRecord[];
  totalElements: number;
}

export const ExceptionsPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [statusFilter, setStatusFilter] = useState<ExceptionRecordStatus | ''>('OPEN');
  const [selectedException, setSelectedException] = useState<ExceptionRecord | null>(null);
  const [resolutionAction, setResolutionAction] = useState('MANUAL_INTERVENTION');
  const [resolutionNotes, setResolutionNotes] = useState('');

  const { data, isLoading } = useQuery<PaginatedExceptions>({
    queryKey: ['exceptions', statusFilter],
    queryFn: () => {
      const url = statusFilter
        ? `/api/v1/exceptions?status=${statusFilter}`
        : '/api/v1/exceptions';
      return apiClient<PaginatedExceptions>(url);
    },
  });

  const assignMutation = useMutation({
    mutationFn: (id: string) =>
      apiClient(`/api/v1/exceptions/${id}/assign`, { method: 'POST' }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['exceptions'] }),
  });

  const resolveMutation = useMutation({
    mutationFn: ({ id, action, notes }: { id: string; action: string; notes: string }) =>
      apiClient(`/api/v1/exceptions/${id}/resolve`, {
        method: 'POST',
        body: JSON.stringify({ action, notes }),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['exceptions'] });
      setSelectedException(null);
      setResolutionNotes('');
    },
  });

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold text-white">Exception Management Board</h1>
          <p className="text-sm text-slate-400">Triage, investigate, and manually resolve system discrepancies (BR-017, BR-018)</p>
        </div>
        <div className="flex gap-2">
          {(['', 'OPEN', 'INVESTIGATING', 'RESOLVED', 'IGNORED'] as const).map((status) => (
            <button
              key={status}
              onClick={() => setStatusFilter(status)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
                statusFilter === status
                  ? 'bg-indigo-600 text-white'
                  : 'bg-slate-800 text-slate-400 hover:text-white'
              }`}
            >
              {status || 'ALL'}
            </button>
          ))}
        </div>
      </div>

      {isLoading ? (
        <div className="text-center py-12 text-slate-500">Loading exception records...</div>
      ) : (
        <div className="border border-slate-800 rounded-xl overflow-hidden bg-slate-950">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-900/80 text-xs uppercase tracking-wider text-slate-400 border-b border-slate-800">
              <tr>
                <th className="py-3 px-4">Type</th>
                <th className="py-3 px-4">Service</th>
                <th className="py-3 px-4">Reference</th>
                <th className="py-3 px-4">Error Message</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800">
              {data?.content.map((rec) => (
                <tr key={rec.id} className="hover:bg-slate-900/50 transition">
                  <td className="py-3 px-4 font-mono text-xs text-rose-400 font-semibold">{rec.exceptionType}</td>
                  <td className="py-3 px-4 text-slate-300">{rec.sourceService}</td>
                  <td className="py-3 px-4 font-mono text-xs text-slate-400">
                    {rec.referenceType}: {rec.referenceId}
                  </td>
                  <td className="py-3 px-4 text-slate-300 max-w-xs truncate">{rec.errorMessage}</td>
                  <td className="py-3 px-4">
                    <span className={`px-2 py-0.5 rounded text-xs font-semibold ${
                      rec.status === 'OPEN'
                        ? 'bg-rose-500/20 text-rose-400'
                        : rec.status === 'INVESTIGATING'
                        ? 'bg-amber-500/20 text-amber-400'
                        : rec.status === 'RESOLVED'
                        ? 'bg-emerald-500/20 text-emerald-400'
                        : 'bg-slate-700 text-slate-300'
                    }`}>
                      {rec.status}
                    </span>
                  </td>
                  <td className="py-3 px-4 text-right space-x-2">
                    {rec.status === 'OPEN' && (
                      <button
                        onClick={() => assignMutation.mutate(rec.id)}
                        className="px-2.5 py-1 text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 rounded"
                      >
                        Investigate
                      </button>
                    )}
                    {(rec.status === 'OPEN' || rec.status === 'INVESTIGATING') && (
                      <button
                        onClick={() => setSelectedException(rec)}
                        className="px-2.5 py-1 text-xs font-semibold bg-indigo-600 hover:bg-indigo-700 text-white rounded"
                      >
                        Resolve
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {data?.content.length === 0 && (
            <div className="py-8 text-center text-slate-500 text-sm">No exceptions matching the criteria.</div>
          )}
        </div>
      )}

      {/* Resolution Modal */}
      {selectedException && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-slate-900 border border-slate-800 rounded-xl max-w-lg w-full p-6 space-y-4 shadow-xl">
            <h3 className="text-lg font-bold text-white">Resolve Exception Record</h3>
            <div className="space-y-1 text-xs text-slate-400 font-mono">
              <p>ID: {selectedException.id}</p>
              <p>Type: {selectedException.exceptionType}</p>
              <p>Ref: {selectedException.referenceType} #{selectedException.referenceId}</p>
            </div>

            <div className="space-y-2">
              <label className="text-xs font-semibold text-slate-300 uppercase">Resolution Action</label>
              <input
                type="text"
                value={resolutionAction}
                onChange={(e) => setResolutionAction(e.target.value)}
                className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-indigo-500"
              />
            </div>

            <div className="space-y-2">
              <label className="text-xs font-semibold text-slate-300 uppercase">Resolution Notes (BR-018 Required)</label>
              <textarea
                rows={3}
                value={resolutionNotes}
                onChange={(e) => setResolutionNotes(e.target.value)}
                placeholder="Document cause of incident and justification for resolution..."
                className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-indigo-500"
              />
            </div>

            <div className="flex justify-end gap-3 pt-2">
              <button
                onClick={() => setSelectedException(null)}
                className="px-4 py-2 text-xs font-semibold text-slate-400 hover:text-white"
              >
                Cancel
              </button>
              <button
                disabled={!resolutionNotes.trim()}
                onClick={() =>
                  resolveMutation.mutate({
                    id: selectedException.id,
                    action: resolutionAction,
                    notes: resolutionNotes,
                  })
                }
                className="px-4 py-2 text-xs font-semibold bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 text-white rounded-lg transition"
              >
                Confirm Resolution
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
