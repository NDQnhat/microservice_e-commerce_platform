import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Layout } from './components/Layout';
import { RbacGuard } from './components/ui/RbacGuard';

// Pages
import { LoginPage } from './pages/LoginPage';
import { DashboardPage } from './pages/DashboardPage';
import { ExceptionsPage } from './pages/ExceptionsPage';
import { OrdersPage } from './pages/OrdersPage';
import { CatalogPage } from './pages/CatalogPage';
import { InventoryPage } from './pages/InventoryPage';
import { PaymentReconciliationPage } from './pages/PaymentReconciliationPage';
import { ShipmentsPage } from './pages/ShipmentsPage';
import { ConfigurationsPage } from './pages/ConfigurationsPage';
import { AuditLogsPage } from './pages/AuditLogsPage';
import { SupportPage } from './pages/SupportPage';
import { RolesManagementPage } from './pages/RolesManagementPage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30 * 1000,
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

export const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />

          <Route path="/" element={<Layout />}>
            {/* 1. Operational Dashboard - All Roles */}
            <Route index element={<DashboardPage />} />

            {/* 2. Exception Board - Super Admin, Ops Admin */}
            <Route
              path="exceptions"
              element={
                <RbacGuard roles={['SUPER_ADMIN', 'OPS_ADMIN', 'ORDER_OPS_ADMIN']} showAccessDenied>
                  <ExceptionsPage />
                </RbacGuard>
              }
            />

            {/* 3. Orders Management - Super Admin, Ops Admin, Order Operator, Support Agent */}
            <Route
              path="orders"
              element={
                <RbacGuard
                  roles={['SUPER_ADMIN', 'OPS_ADMIN', 'ORDER_OPS_ADMIN', 'ORDER_OPERATOR', 'SUPPORT_AGENT']}
                  showAccessDenied
                >
                  <OrdersPage />
                </RbacGuard>
              }
            />

            {/* 4. Catalog & Pricing - Super Admin, Catalog Manager */}
            <Route
              path="catalog"
              element={
                <RbacGuard roles={['SUPER_ADMIN', 'CATALOG_MANAGER']} showAccessDenied>
                  <CatalogPage />
                </RbacGuard>
              }
            />

            {/* 5. Inventory & Audit - Super Admin, Warehouse Staff, Ops Admin */}
            <Route
              path="inventory"
              element={
                <RbacGuard roles={['SUPER_ADMIN', 'WAREHOUSE_STAFF', 'OPS_ADMIN']} showAccessDenied>
                  <InventoryPage />
                </RbacGuard>
              }
            />

            {/* 6. Payment Reconciliation - Super Admin, Financial Auditor, Ops Admin */}
            <Route
              path="reconciliation"
              element={
                <RbacGuard roles={['SUPER_ADMIN', 'OPS_ADMIN', 'FINANCIAL_AUDITOR']} showAccessDenied>
                  <PaymentReconciliationPage />
                </RbacGuard>
              }
            />

            {/* 7. Fulfillment & Shipments - Super Admin, Warehouse Staff, Ops Admin */}
            <Route
              path="shipments"
              element={
                <RbacGuard roles={['SUPER_ADMIN', 'WAREHOUSE_STAFF', 'OPS_ADMIN']} showAccessDenied>
                  <ShipmentsPage />
                </RbacGuard>
              }
            />

            {/* 8. Dynamic Configuration - Super Admin */}
            <Route
              path="configurations"
              element={
                <RbacGuard roles={['SUPER_ADMIN']} showAccessDenied>
                  <ConfigurationsPage />
                </RbacGuard>
              }
            />

            {/* 9. Compliance Audit Logs - Super Admin */}
            <Route
              path="audit-logs"
              element={
                <RbacGuard roles={['SUPER_ADMIN']} showAccessDenied>
                  <AuditLogsPage />
                </RbacGuard>
              }
            />

            {/* 10. Customer Support Operations - Super Admin, Support Agent, Customer Support */}
            <Route
              path="support"
              element={
                <RbacGuard roles={['SUPER_ADMIN', 'SUPPORT_AGENT', 'CUSTOMER_SUPPORT']} showAccessDenied>
                  <SupportPage />
                </RbacGuard>
              }
            />

            {/* 11. Role & Permission Management - Super Admin (FR-033, BR-018) */}
            <Route
              path="roles"
              element={
                <RbacGuard roles={['SUPER_ADMIN']} showAccessDenied>
                  <RolesManagementPage />
                </RbacGuard>
              }
            />

            {/* Fallback */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
};
export default App;
