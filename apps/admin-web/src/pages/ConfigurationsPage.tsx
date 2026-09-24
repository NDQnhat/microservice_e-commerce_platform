import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';
import { BusinessConfiguration } from '@/types';
import { Sliders, CheckCircle, Edit3 } from 'lucide-react';

export const ConfigurationsPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [selectedConfig, setSelectedConfig] = useState<BusinessConfiguration | null>(null);
  const [newValue, setNewValue] = useState('');
  const [reason, setReason] = useState('');

  const { data: configs, isLoading } = useQuery<BusinessConfiguration[]>({
    queryKey: ['configurations'],
    queryFn: () => apiClient<BusinessConfiguration[]>('/api/v1/configurations'),
  });

  const updateMutation = useMutation({
    mutationFn: (payload: { configKey: string; configValue: string; description?: string; reason: string }) =>
      apiClient('/api/v1/configurations', {
        method: 'POST',
        body: JSON.stringify(payload),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['configurations'] });
      setSelectedConfig(null);
      setNewValue('');
      setReason('');
    },
  });

  const handleEdit = (cfg: BusinessConfiguration) => {
    setSelectedConfig(cfg);
    setNewValue(cfg.configValue);
    setReason('');
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-white">Dynamic Business Configuration</h1>
        <p className="text-sm text-slate-400">Manage runtime system thresholds with immutable versioning (API-CFG-001, BR-019)</p>
      </div>

      {isLoading ? (
        <div className="text-center py-12 text-slate-500">Loading configurations...</div>
      ) : (
        <div className="border border-slate-800 rounded-xl overflow-hidden bg-slate-950">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-900/80 text-xs uppercase tracking-wider text-slate-400 border-b border-slate-800">
              <tr>
                <th className="py-3 px-4">Key</th>
                <th className="py-3 px-4">Current Value</th>
                <th className="py-3 px-4">Description</th>
                <th className="py-3 px-4">Version</th>
                <th className="py-3 px-4">Last Updated By</th>
                <th className="py-3 px-4 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800">
              {configs?.map((cfg) => (
                <tr key={cfg.id} className="hover:bg-slate-900/50 transition">
                  <td className="py-3 px-4 font-mono text-xs text-indigo-400 font-semibold">{cfg.configKey}</td>
                  <td className="py-3 px-4 font-mono text-xs text-white bg-slate-900/50 px-2 py-1 rounded max-w-xs truncate">
                    {cfg.configValue}
                  </td>
                  <td className="py-3 px-4 text-slate-400 text-xs">{cfg.description || '-'}</td>
                  <td className="py-3 px-4">
                    <span className="px-2 py-0.5 rounded text-xs font-semibold bg-indigo-500/20 text-indigo-300">
                      v{cfg.version}
                    </span>
                  </td>
                  <td className="py-3 px-4 text-xs text-slate-400">{cfg.createdBy}</td>
                  <td className="py-3 px-4 text-right">
                    <button
                      onClick={() => handleEdit(cfg)}
                      className="px-2.5 py-1 text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-white rounded flex items-center gap-1.5 ml-auto"
                    >
                      <Edit3 className="h-3 w-3" /> Edit
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {selectedConfig && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-slate-900 border border-slate-800 rounded-xl max-w-lg w-full p-6 space-y-4 shadow-xl">
            <h3 className="text-lg font-bold text-white">Update Configuration (v{selectedConfig.version + 1})</h3>
            <p className="text-xs text-slate-400 font-mono">Key: {selectedConfig.configKey}</p>

            <div className="space-y-2">
              <label className="text-xs font-semibold text-slate-300 uppercase">New Value</label>
              <textarea
                rows={3}
                value={newValue}
                onChange={(e) => setNewValue(e.target.value)}
                className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-indigo-500"
              />
            </div>

            <div className="space-y-2">
              <label className="text-xs font-semibold text-slate-300 uppercase">Reason for Change (BR-019 Required)</label>
              <input
                type="text"
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                placeholder="Audit justification for change..."
                className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-indigo-500"
              />
            </div>

            <div className="flex justify-end gap-3 pt-2">
              <button
                onClick={() => setSelectedConfig(null)}
                className="px-4 py-2 text-xs font-semibold text-slate-400 hover:text-white"
              >
                Cancel
              </button>
              <button
                disabled={!newValue.trim() || !reason.trim()}
                onClick={() =>
                  updateMutation.mutate({
                    configKey: selectedConfig.configKey,
                    configValue: newValue,
                    description: selectedConfig.description,
                    reason,
                  })
                }
                className="px-4 py-2 text-xs font-semibold bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white rounded-lg transition"
              >
                Save Version {selectedConfig.version + 1}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
