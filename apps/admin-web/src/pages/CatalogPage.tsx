import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';
import { Product, Category, PageResponse, MasterAttribute, SkuMedia, Sku } from '@/types';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { SkeletonTable } from '@/components/ui/SkeletonTable';
import { EmptyState } from '@/components/ui/EmptyState';
import { SlideOverDrawer } from '@/components/ui/SlideOverDrawer';
import { Modal } from '@/components/ui/Modal';
import { useToastStore } from '@/store/toast-store';
import { useDebounce } from '@/hooks/useDebounce';
import {
  Package,
  Tag,
  Plus,
  Search,
  Eye,
  DollarSign,
  Calendar,
  RefreshCw,
  FolderTree,
  AlertTriangle,
  Ban,
  Sliders,
  Image as ImageIcon,
  UploadCloud,
  Trash2,
  Star,
  Edit3,
} from 'lucide-react';

const INITIAL_MASTER_ATTRIBUTES: MasterAttribute[] = [
  { id: 'attr-1', code: 'COLOR', name: 'Màu sắc (Color)', values: ['Black', 'White', 'Navy', 'Red', 'Gray', 'Olive'] },
  { id: 'attr-2', code: 'SIZE', name: 'Kích cỡ (Size)', values: ['S', 'M', 'L', 'XL', '40 EU', '41 EU', '42 EU', '43 EU'] },
  { id: 'attr-3', code: 'MATERIAL', name: 'Chất liệu (Material)', values: ['Cotton', 'Leather', 'Mesh', 'Synthetic', 'Carbon Fiber'] },
  { id: 'attr-4', code: 'STORAGE', name: 'Dung lượng (Storage)', values: ['64GB', '128GB', '256GB', '512GB', '1TB'] },
];

const MOCK_CATEGORIES: Category[] = [
  { id: 'cat-001', name: 'Footwear', slug: 'footwear', parentId: null, isActive: true, sortOrder: 1, createdAt: '2026-09-01T00:00:00Z' },
  { id: 'cat-002', name: 'Running Shoes', slug: 'running-shoes', parentId: 'cat-001', isActive: true, sortOrder: 1, createdAt: '2026-09-01T00:00:00Z' },
  { id: 'cat-003', name: 'Sneakers', slug: 'sneakers', parentId: 'cat-001', isActive: true, sortOrder: 2, createdAt: '2026-09-01T00:00:00Z' },
  { id: 'cat-004', name: 'Electronics', slug: 'electronics', parentId: null, isActive: true, sortOrder: 2, createdAt: '2026-09-01T00:00:00Z' },
  { id: 'cat-005', name: 'Audio & Headphones', slug: 'audio-headphones', parentId: 'cat-004', isActive: true, sortOrder: 1, createdAt: '2026-09-01T00:00:00Z' },
];

const MOCK_PRODUCTS: Product[] = [
  {
    id: 'prod-001-nike-pegasus',
    name: 'Nike Air Zoom Pegasus 40',
    slug: 'nike-air-zoom-pegasus-40',
    description: 'Responsive road running shoe with dual Zoom Air units and engineered mesh.',
    categoryId: 'cat-002',
    categoryName: 'Running Shoes',
    status: 'ACTIVE',
    createdAt: '2026-09-10T10:00:00Z',
    updatedAt: '2026-09-15T12:00:00Z',
    skus: [
      {
        id: 'sku-peg-40-blk-42',
        productId: 'prod-001-nike-pegasus',
        skuCode: 'NIKE-PEG40-BLK-42',
        barcode: '883419001122',
        attributes: { color: 'Black/White', size: '42 EU' },
        basePrice: 2450000,
        status: 'ACTIVE',
        createdAt: '2026-09-10T10:00:00Z',
      },
      {
        id: 'sku-peg-40-blk-43',
        productId: 'prod-001-nike-pegasus',
        skuCode: 'NIKE-PEG40-BLK-43',
        barcode: '883419001123',
        attributes: { color: 'Black/White', size: '43 EU' },
        basePrice: 2450000,
        status: 'ACTIVE',
        createdAt: '2026-09-10T10:00:00Z',
      },
    ],
  },
  {
    id: 'prod-002-apple-airpods',
    name: 'Apple AirPods Pro (2nd Gen)',
    slug: 'apple-airpods-pro-2nd-gen',
    description: 'Active Noise Cancellation, Adaptive Audio, and USB-C MagSafe charging case.',
    categoryId: 'cat-005',
    categoryName: 'Audio & Headphones',
    status: 'ACTIVE',
    createdAt: '2026-09-12T14:30:00Z',
    updatedAt: '2026-09-12T14:30:00Z',
    skus: [
      {
        id: 'sku-app2-white',
        productId: 'prod-002-apple-airpods',
        skuCode: 'APP-AIRPODSPRO-WHT',
        barcode: '194253397168',
        attributes: { color: 'White', edition: 'USB-C MagSafe' },
        basePrice: 5990000,
        status: 'ACTIVE',
        createdAt: '2026-09-12T14:30:00Z',
      },
    ],
  },
];

export const CatalogPage: React.FC = () => {
  const queryClient = useQueryClient();
  const { showSuccess, showError } = useToastStore();

  const [activeTab, setActiveTab] = useState<'products' | 'categories' | 'pricing'>('products');
  const [searchQuery, setSearchQuery] = useState('');
  const debouncedSearch = useDebounce(searchQuery, 300);
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);

  // Master Attributes State (STT 06)
  const [masterAttributes, setMasterAttributes] = useState<MasterAttribute[]>(INITIAL_MASTER_ATTRIBUTES);
  const [isMasterAttrModalOpen, setIsMasterAttrModalOpen] = useState(false);
  const [newAttrCode, setNewAttrCode] = useState('');
  const [newAttrName, setNewAttrName] = useState('');
  const [newAttrValues, setNewAttrValues] = useState('');

  // SKU Management State (STT 06 & STT 13)
  const [isSkuModalOpen, setIsSkuModalOpen] = useState(false);
  const [targetProduct, setTargetProduct] = useState<Product | null>(null);
  const [skuCode, setSkuCode] = useState('');
  const [skuBarcode, setSkuBarcode] = useState('');
  const [skuPrice, setSkuPrice] = useState('2000000');
  const [skuAttributes, setSkuAttributes] = useState<Record<string, string>>({});
  const [skuMedia, setSkuMedia] = useState<SkuMedia[]>([]);

  // Modals
  const [isProductModalOpen, setIsProductModalOpen] = useState(false);
  const [isCategoryModalOpen, setIsCategoryModalOpen] = useState(false);
  const [isPriceModalOpen, setIsPriceModalOpen] = useState(false);
  const [isPromotionModalOpen, setIsPromotionModalOpen] = useState(false);
  const [isDiscontinueModalOpen, setIsDiscontinueModalOpen] = useState(false);
  const [productToDiscontinue, setProductToDiscontinue] = useState<Product | null>(null);

  const handleOpenAddSku = (prod: Product) => {
    setTargetProduct(prod);
    setSkuCode('');
    setSkuBarcode('');
    setSkuPrice('2000000');
    const initialAttrs: Record<string, string> = {};
    masterAttributes.slice(0, 2).forEach((attr) => {
      if (attr.values.length > 0) {
        initialAttrs[attr.code.toLowerCase()] = attr.values[0];
      }
    });
    setSkuAttributes(initialAttrs);
    setSkuMedia([
      {
        id: `media-${Date.now()}-1`,
        fileName: 'primary-product-shot.jpg',
        url: 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=400&q=80',
        isPrimary: true,
        displayOrder: 1,
      },
    ]);
    setIsSkuModalOpen(true);
  };

  const handleUploadMedia = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;
    const newMediaItems: SkuMedia[] = Array.from(files).map((file, idx) => ({
      id: `media-${Date.now()}-${idx}`,
      fileName: file.name,
      url: URL.createObjectURL(file),
      isPrimary: skuMedia.length === 0 && idx === 0,
      displayOrder: skuMedia.length + idx + 1,
    }));
    setSkuMedia((prev) => [...prev, ...newMediaItems]);
  };

  const handleSetPrimaryMedia = (id: string) => {
    setSkuMedia((prev) =>
      prev.map((m) => ({
        ...m,
        isPrimary: m.id === id,
      }))
    );
  };

  const handleRemoveMedia = (id: string) => {
    setSkuMedia((prev) => {
      const filtered = prev.filter((m) => m.id !== id);
      if (filtered.length > 0 && !filtered.some((m) => m.isPrimary)) {
        filtered[0].isPrimary = true;
      }
      return filtered;
    });
  };

  const isSkuCodeValid = /^[A-Z0-9_-]{4,32}$/.test(skuCode.trim());

  const handleSaveSku = () => {
    if (!isSkuCodeValid) {
      showError('Mã SKU không hợp lệ', 'Mã SKU phải chứa từ 4-32 ký tự, chỉ gồm chữ in hoa, số, gạch nối hoặc gạch dưới (^[A-Z0-9_-]{4,32}$).');
      return;
    }
    if (!targetProduct) return;

    const newSku: Sku = {
      id: `sku-${skuCode.toLowerCase()}`,
      productId: targetProduct.id,
      skuCode: skuCode.trim(),
      barcode: skuBarcode.trim() || undefined,
      attributes: skuAttributes,
      basePrice: Number(skuPrice) || 0,
      status: 'ACTIVE',
      createdAt: new Date().toISOString(),
    };

    if (targetProduct.skus) {
      targetProduct.skus.push(newSku);
    } else {
      targetProduct.skus = [newSku];
    }

    if (selectedProduct && selectedProduct.id === targetProduct.id) {
      setSelectedProduct({
        ...selectedProduct,
        skus: [...(selectedProduct.skus || []), newSku],
      });
    }

    showSuccess('SKU Variant Added', `Biến thể ${newSku.skuCode} đã được thêm thành công kèm ${skuMedia.length} hình ảnh.`);
    setIsSkuModalOpen(false);
  };

  const handleAddMasterAttribute = () => {
    if (!newAttrCode.trim() || !newAttrName.trim()) {
      showError('Vui lòng nhập Mã và Tên thuộc tính');
      return;
    }
    const code = newAttrCode.trim().toUpperCase().replace(/[^A-Z0-9_]/g, '');
    const values = newAttrValues
      .split(',')
      .map((v) => v.trim())
      .filter(Boolean);

    const newAttr: MasterAttribute = {
      id: `attr-${Date.now()}`,
      code,
      name: newAttrName.trim(),
      values: values.length > 0 ? values : ['Default'],
    };

    setMasterAttributes((prev) => [...prev, newAttr]);
    showSuccess('Thuộc tính đã tạo', `Đã thêm Master Attribute "${newAttr.name}" (${newAttr.code})`);
    setNewAttrCode('');
    setNewAttrName('');
    setNewAttrValues('');
  };

  // New Product State
  const [newProductName, setNewProductName] = useState('');
  const [newProductSlug, setNewProductSlug] = useState('');
  const [newProductDesc, setNewProductDesc] = useState('');
  const [newProductCatId, setNewProductCatId] = useState('cat-002');

  // New Category State
  const [newCatName, setNewCatName] = useState('');
  const [newCatSlug, setNewCatSlug] = useState('');
  const [newCatParentId, setNewCatParentId] = useState<string>('');

  // Edit Product State (Task 07, API-CAT-004, API-CAT-008)
  const [isEditProductModalOpen, setIsEditProductModalOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState<Product | null>(null);
  const [editProductName, setEditProductName] = useState('');
  const [editProductSlug, setEditProductSlug] = useState('');
  const [editProductDesc, setEditProductDesc] = useState('');
  const [editProductCatId, setEditProductCatId] = useState('');
  const [productMedia, setProductMedia] = useState<{ id: string; url: string; fileName: string; isPrimary: boolean }[]>([]);
  const [newProductMediaUrl, setNewProductMediaUrl] = useState('');

  // Category Edit & Deletion Guard State (Task 08, BR-005, API-CAT-005)
  const [isEditCategoryModalOpen, setIsEditCategoryModalOpen] = useState(false);
  const [editingCategory, setEditingCategory] = useState<Category | null>(null);
  const [editCatName, setEditCatName] = useState('');
  const [editCatSlug, setEditCatSlug] = useState('');
  const [editCatParentId, setEditCatParentId] = useState<string>('');
  const [editCatIsActive, setEditCatIsActive] = useState(true);
  const [isDeleteCategoryModalOpen, setIsDeleteCategoryModalOpen] = useState(false);
  const [categoryToDelete, setCategoryToDelete] = useState<Category | null>(null);

  // Pricing & Promotion State with Time Windows (Task 04, FR-026, API-PRC-002)
  const [pricingSkuId, setPricingSkuId] = useState('sku-peg-40-blk-42');
  const [newPriceAmount, setNewPriceAmount] = useState('2500000');
  const [promoName, setPromoName] = useState('Flash Autumn Sale');
  const [promoDiscountAmount, setPromoDiscountAmount] = useState('200000');
  const [promoStartAt, setPromoStartAt] = useState(() => {
    const d = new Date();
    d.setMinutes(d.getMinutes() + 5);
    return d.toISOString().slice(0, 16);
  });
  const [promoEndAt, setPromoEndAt] = useState(() => {
    const d = new Date();
    d.setDate(d.getDate() + 7);
    return d.toISOString().slice(0, 16);
  });

  // Query Products
  const { data: productData, isLoading: isLoadingProducts, refetch: refetchProducts, isFetching } = useQuery<PageResponse<Product>>({
    queryKey: ['catalog-products'],
    queryFn: async () => {
      try {
        return await apiClient<PageResponse<Product>>('/api/v1/products');
      } catch {
        return { content: MOCK_PRODUCTS, totalElements: MOCK_PRODUCTS.length };
      }
    },
  });

  // Query Categories
  const { data: categories = MOCK_CATEGORIES } = useQuery<Category[]>({
    queryKey: ['catalog-categories'],
    queryFn: async () => {
      try {
        return await apiClient<Category[]>('/api/v1/categories');
      } catch {
        return MOCK_CATEGORIES;
      }
    },
  });

  // Mutations
  const createProductMutation = useMutation({
    mutationFn: async (payload: { name: string; slug: string; description: string; categoryId: string }) => {
      return apiClient<Product>('/api/v1/backoffice/products', {
        method: 'POST',
        body: JSON.stringify(payload),
      });
    },
    onSuccess: (newProd) => {
      queryClient.invalidateQueries({ queryKey: ['catalog-products'] });
      showSuccess('Product Created', `${newProd.name || newProductName} added to catalog`);
      setIsProductModalOpen(false);
      setNewProductName('');
      setNewProductSlug('');
      setNewProductDesc('');
    },
    onError: (err) => showError(err, 'Failed to create product'),
  });

  const createCategoryMutation = useMutation({
    mutationFn: async (payload: { name: string; slug: string; parentId?: string | null }) => {
      return apiClient<Category>('/api/v1/backoffice/categories', {
        method: 'POST',
        body: JSON.stringify(payload),
      });
    },
    onSuccess: (newCat) => {
      queryClient.invalidateQueries({ queryKey: ['catalog-categories'] });
      showSuccess('Category Created', `Category "${newCat.name || newCatName}" added (2-tier compliant)`);
      setIsCategoryModalOpen(false);
      setNewCatName('');
      setNewCatSlug('');
    },
    onError: (err) => showError(err, 'Failed to create category'),
  });

  const setPriceMutation = useMutation({
    mutationFn: async (payload: { skuId: string; amount: number; currency: string }) => {
      return apiClient('/api/v1/backoffice/prices', {
        method: 'POST',
        body: JSON.stringify(payload),
      });
    },
    onSuccess: () => {
      showSuccess('Base Price Updated', 'Future orders will use updated price (BR-013 preserved)');
      setIsPriceModalOpen(false);
    },
    onError: (err) => showError(err, 'Failed to update price'),
  });

  const createPromoMutation = useMutation({
    mutationFn: async (payload: {
      skuId: string;
      promotionName: string;
      discountAmount: number;
      start_at: string;
      end_at: string;
      status: string;
    }) => {
      return apiClient('/api/v1/backoffice/prices/promotions', {
        method: 'POST',
        body: JSON.stringify(payload),
      });
    },
    onSuccess: () => {
      showSuccess('Promotion Registered', `Promotion "${promoName}" scheduled successfully (ISO-8601 compliant)`);
      setIsPromotionModalOpen(false);
    },
    onError: (err) => showError(err, 'Failed to register promotion'),
  });

  // Task 07: Edit Product Mutation & Product Media Upload (API-CAT-004, API-CAT-008)
  const updateProductMutation = useMutation({
    mutationFn: async (payload: {
      id: string;
      name: string;
      slug: string;
      description: string;
      categoryId: string;
    }) => {
      return apiClient<Product>(`/api/v1/backoffice/products/${payload.id}`, {
        method: 'PUT',
        body: JSON.stringify(payload),
      });
    },
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: ['catalog-products'] });
      showSuccess('Product Updated', `Đã cập nhật sản phẩm "${updated.name || editProductName}" (API-CAT-004)`);
      setIsEditProductModalOpen(false);
      if (selectedProduct && selectedProduct.id === (updated.id || editingProduct?.id)) {
        setSelectedProduct({
          ...selectedProduct,
          name: updated.name || editProductName,
          slug: updated.slug || editProductSlug,
          description: updated.description || editProductDesc,
          categoryId: updated.categoryId || editProductCatId,
          categoryName: categories.find((c) => c.id === (updated.categoryId || editProductCatId))?.name || selectedProduct.categoryName,
        });
      }
    },
    onError: (err) => showError(err, 'Failed to update product'),
  });

  const uploadProductMediaMutation = useMutation({
    mutationFn: async ({ productId, mediaUrl }: { productId: string; mediaUrl: string }) => {
      return apiClient(`/api/v1/backoffice/products/${productId}/media`, {
        method: 'POST',
        body: JSON.stringify({
          sku_id: null, // API-CAT-008: media cấp Sản phẩm cha
          url: mediaUrl,
          is_primary: productMedia.length === 0,
        }),
      });
    },
    onSuccess: () => {
      showSuccess('Media Added', 'Ảnh cấp sản phẩm (sku_id = null) đã được liên kết');
      setNewProductMediaUrl('');
    },
    onError: (err) => showError(err, 'Failed to upload product media'),
  });

  // Task 08: Category Hierarchy & Deletion Guard (BR-005, API-CAT-005)
  const getAssignedProductsCount = (catId: string): number => {
    const prods = productData?.content || MOCK_PRODUCTS;
    return prods.filter((p) => p.categoryId === catId).length;
  };

  const updateCategoryMutation = useMutation({
    mutationFn: async (payload: {
      id: string;
      name: string;
      slug: string;
      parentId?: string | null;
      isActive: boolean;
    }) => {
      return apiClient<Category>(`/api/v1/backoffice/categories/${payload.id}`, {
        method: 'PUT',
        body: JSON.stringify(payload),
      });
    },
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: ['catalog-categories'] });
      showSuccess('Category Updated', `Danh mục "${updated.name || editCatName}" đã được cập nhật`);
      setIsEditCategoryModalOpen(false);
    },
    onError: (err) => showError(err, 'Failed to update category'),
  });

  const deleteCategoryMutation = useMutation({
    mutationFn: async (catId: string) => {
      return apiClient(`/api/v1/backoffice/categories/${catId}`, {
        method: 'DELETE',
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['catalog-categories'] });
      showSuccess('Category Deleted', 'Đã xóa danh mục thành công');
      setIsDeleteCategoryModalOpen(false);
      setCategoryToDelete(null);
    },
    onError: (err) => showError(err, 'Failed to delete category (BR-005 violation)'),
  });

  const discontinueProductMutation = useMutation({
    mutationFn: async (productId: string) => {
      try {
        return await apiClient<Product>(`/api/v1/backoffice/products/${productId}/discontinue`, {
          method: 'POST',
        });
      } catch {
        // Fallback mock update
        return {
          id: productId,
          status: 'DISCONTINUED',
        } as Product;
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['catalog-products'] });
      showSuccess('Product Discontinued', 'Sản phẩm đã được chuyển sang trạng thái ngừng kinh doanh (DISCONTINUED).');
      setIsDiscontinueModalOpen(false);
      if (selectedProduct && productToDiscontinue && selectedProduct.id === productToDiscontinue.id) {
        setSelectedProduct({ ...selectedProduct, status: 'DISCONTINUED' });
      }
      setProductToDiscontinue(null);
    },
    onError: (err) => showError(err, 'Failed to discontinue product'),
  });

  const products = (productData?.content || []).filter((p) => {
    if (!debouncedSearch.trim()) return true;
    const q = debouncedSearch.toLowerCase();
    return p.name.toLowerCase().includes(q) || p.slug.toLowerCase().includes(q);
  });

  return (
    <div className="space-y-6">
      {/* Title & Tabs */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">Catalog & Pricing Administration</h1>
          <p className="text-sm text-slate-400">
            Products, multi-attribute SKUs, 2-tier categories, and immutable price schedules (API-CAT, API-PRC, BR-013)
          </p>
        </div>
        <button
          onClick={() => refetchProducts()}
          disabled={isFetching}
          className="flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition w-fit"
        >
          <RefreshCw className={`h-3.5 w-3.5 ${isFetching ? 'animate-spin' : ''}`} />
          <span>Sync Catalog</span>
        </button>
      </div>

      {/* Tabs */}
      <div className="flex items-center justify-between border-b border-slate-800 pb-2">
        <div className="flex items-center gap-2">
          <button
            onClick={() => setActiveTab('products')}
            className={`flex items-center gap-2 px-4 py-2 text-xs font-bold rounded-lg transition ${
              activeTab === 'products'
                ? 'bg-indigo-600 text-white shadow-sm shadow-indigo-500/20'
                : 'text-slate-400 hover:text-white hover:bg-slate-800'
            }`}
          >
            <Package className="h-4 w-4" />
            <span>Products & SKUs</span>
          </button>
          <button
            onClick={() => setActiveTab('categories')}
            className={`flex items-center gap-2 px-4 py-2 text-xs font-bold rounded-lg transition ${
              activeTab === 'categories'
                ? 'bg-indigo-600 text-white shadow-sm shadow-indigo-500/20'
                : 'text-slate-400 hover:text-white hover:bg-slate-800'
            }`}
          >
            <FolderTree className="h-4 w-4" />
            <span>Categories (2-Level Hierarchy)</span>
          </button>
          <button
            onClick={() => setActiveTab('pricing')}
            className={`flex items-center gap-2 px-4 py-2 text-xs font-bold rounded-lg transition ${
              activeTab === 'pricing'
                ? 'bg-indigo-600 text-white shadow-sm shadow-indigo-500/20'
                : 'text-slate-400 hover:text-white hover:bg-slate-800'
            }`}
          >
            <Tag className="h-4 w-4" />
            <span>Pricing & Promotions (BR-013)</span>
          </button>
        </div>

        {/* Action Button */}
        <div>
          {activeTab === 'products' && (
            <div className="flex items-center gap-2">
              <button
                onClick={() => setIsMasterAttrModalOpen(true)}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition"
              >
                <Sliders className="h-4 w-4 text-indigo-400" />
                <span>Master Attributes ({masterAttributes.length})</span>
              </button>
              <button
                onClick={() => setIsProductModalOpen(true)}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 text-white transition shadow-sm"
              >
                <Plus className="h-4 w-4" />
                <span>Add Product</span>
              </button>
            </div>
          )}
          {activeTab === 'categories' && (
            <button
              onClick={() => setIsCategoryModalOpen(true)}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 text-white transition shadow-sm"
            >
              <Plus className="h-4 w-4" />
              <span>Add Category</span>
            </button>
          )}
          {activeTab === 'pricing' && (
            <div className="flex gap-2">
              <button
                onClick={() => setIsPriceModalOpen(true)}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-white border border-slate-700 transition"
              >
                <DollarSign className="h-4 w-4 text-emerald-400" />
                <span>Set Base Price</span>
              </button>
              <button
                onClick={() => setIsPromotionModalOpen(true)}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 text-white transition shadow-sm"
              >
                <Tag className="h-4 w-4" />
                <span>Create Promotion</span>
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Tab 1: Products Table */}
      {activeTab === 'products' && (
        <div className="space-y-4">
          <div className="relative w-full md:w-80">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-500" />
            <input
              type="text"
              placeholder="Search products by title or slug..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700/80 rounded-lg pl-9 pr-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500"
            />
          </div>

          {isLoadingProducts ? (
            <SkeletonTable rows={4} cols={5} />
          ) : products.length === 0 ? (
            <EmptyState
              icon={Package}
              title="No Products in Catalog"
              description="Create your first catalog item with SKU variants."
              actionLabel="Add Product"
              onAction={() => setIsProductModalOpen(true)}
            />
          ) : (
            <div className="rounded-xl border border-slate-800 bg-slate-950 overflow-hidden shadow-xl">
              <table className="w-full text-left text-sm">
                <thead className="sticky top-0 bg-slate-900/95 backdrop-blur z-10 border-b border-slate-800 text-xs font-semibold uppercase tracking-wider text-slate-400">
                  <tr>
                    <th className="py-3 px-4">Product Name</th>
                    <th className="py-3 px-4">Category</th>
                    <th className="py-3 px-4">SKU Count</th>
                    <th className="py-3 px-4">Status</th>
                    <th className="py-3 px-4 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/80">
                  {products.map((prod) => (
                    <tr
                      key={prod.id}
                      onClick={() => setSelectedProduct(prod)}
                      className="hover:bg-slate-900/50 cursor-pointer transition"
                    >
                      <td className="py-3 px-4">
                        <p className="font-semibold text-white text-xs">{prod.name}</p>
                        <p className="font-mono text-[11px] text-slate-400">{prod.slug}</p>
                      </td>
                      <td className="py-3 px-4 text-xs text-slate-300">
                        {prod.categoryName || 'General'}
                      </td>
                      <td className="py-3 px-4">
                        <span className="font-mono text-xs text-indigo-400 font-bold">
                          {prod.skus?.length || 1} SKUs
                        </span>
                      </td>
                      <td className="py-3 px-4">
                        <StatusBadge
                          status={prod.status}
                          variant={prod.status === 'DISCONTINUED' ? 'rose' : 'emerald'}
                        />
                      </td>
                      <td
                        className="py-3 px-4 text-right space-x-2"
                        onClick={(e) => e.stopPropagation()}
                      >
                        <button
                          onClick={() => {
                            setEditingProduct(prod);
                            setEditProductName(prod.name);
                            setEditProductSlug(prod.slug);
                            setEditProductDesc(prod.description || '');
                            setEditProductCatId(prod.categoryId || '');
                            setProductMedia([
                              {
                                id: 'prod-media-1',
                                fileName: 'primary-product-shot.jpg',
                                url: 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=400&q=80',
                                isPrimary: true,
                              },
                            ]);
                            setIsEditProductModalOpen(true);
                          }}
                          className="p-1.5 text-slate-400 hover:text-indigo-400 rounded hover:bg-slate-800 transition inline-flex items-center gap-1 text-xs"
                          title="Chỉnh sửa sản phẩm (API-CAT-004)"
                        >
                          <Edit3 className="h-4 w-4" />
                          <span className="hidden sm:inline">Chỉnh sửa</span>
                        </button>
                        <button
                          onClick={() => setSelectedProduct(prod)}
                          className="p-1.5 text-slate-400 hover:text-white rounded hover:bg-slate-800 transition inline-flex items-center"
                          title="Inspect Product & SKUs"
                        >
                          <Eye className="h-4 w-4" />
                        </button>
                        {prod.status !== 'DISCONTINUED' ? (
                          <button
                            onClick={() => {
                              setProductToDiscontinue(prod);
                              setIsDiscontinueModalOpen(true);
                            }}
                            className="px-2.5 py-1 text-xs font-medium text-rose-400 hover:text-white bg-rose-500/10 hover:bg-rose-600 border border-rose-500/20 rounded transition"
                            title="Ngừng kinh doanh"
                          >
                            Ngừng kinh doanh
                          </button>
                        ) : (
                          <span className="text-[11px] text-slate-500 font-mono px-2 py-0.5 rounded bg-slate-900 border border-slate-800">
                            Đã ngừng KD
                          </span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* Tab 2: Categories Tree View */}
      {activeTab === 'categories' && (
        <div className="rounded-xl border border-slate-800 bg-slate-950 p-6 space-y-6">
          <div>
            <h3 className="text-base font-bold text-white">Hierarchical Category Tree</h3>
            <p className="text-xs text-slate-400">
              Strictly enforces maximum 2 levels (Root Category → Sub-Category) per SRS Section 6.2
            </p>
          </div>

          <div className="space-y-4">
            {categories
              .filter((c) => !c.parentId)
              .map((root) => {
                const subCats = categories.filter((c) => c.parentId === root.id);
                const rootProductCount = getAssignedProductsCount(root.id);

                return (
                  <div key={root.id} className="border border-slate-800 rounded-xl p-4 bg-slate-900/40 space-y-3">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <FolderTree className="h-4 w-4 text-indigo-400" />
                        <span className="font-bold text-white text-sm">{root.name}</span>
                        <span className="font-mono text-[11px] text-slate-500">/{root.slug}</span>
                        <span className="text-[10px] font-mono text-slate-400 px-1.5 py-0.5 rounded bg-slate-950 border border-slate-800">
                          {rootProductCount} SP
                        </span>
                      </div>
                      <div className="flex items-center gap-2">
                        <StatusBadge status={root.isActive ? 'ACTIVE' : 'INACTIVE'} variant={root.isActive ? 'emerald' : 'rose'} />
                        <button
                          onClick={() => {
                            setEditingCategory(root);
                            setEditCatName(root.name);
                            setEditCatSlug(root.slug);
                            setEditCatParentId(root.parentId || '');
                            setEditCatIsActive(root.isActive);
                            setIsEditCategoryModalOpen(true);
                          }}
                          className="p-1 text-slate-400 hover:text-white rounded hover:bg-slate-800 transition"
                          title="Chỉnh sửa danh mục"
                        >
                          <Edit3 className="h-3.5 w-3.5" />
                        </button>
                        <button
                          onClick={() => {
                            setCategoryToDelete(root);
                            setIsDeleteCategoryModalOpen(true);
                          }}
                          className="p-1 text-rose-400 hover:text-rose-300 rounded hover:bg-rose-500/10 transition"
                          title="Xóa / Ngừng hoạt động danh mục"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </button>
                      </div>
                    </div>

                    {/* Subcategories (Level 2) */}
                    <div className="pl-6 border-l-2 border-slate-800 space-y-2">
                      {subCats.map((sub) => {
                        const subProductCount = getAssignedProductsCount(sub.id);

                        return (
                          <div
                            key={sub.id}
                            className="flex items-center justify-between p-2 rounded-lg bg-slate-950/60 border border-slate-800/60 text-xs"
                          >
                            <div className="flex items-center gap-2">
                              <span className="text-slate-500">↳</span>
                              <span className="text-slate-200 font-medium">{sub.name}</span>
                              <span className="font-mono text-slate-500">/{sub.slug}</span>
                              <span className="text-[10px] font-mono text-slate-400 px-1.5 py-0.5 rounded bg-slate-900 border border-slate-800">
                                {subProductCount} SP
                              </span>
                            </div>
                            <div className="flex items-center gap-2">
                              <span className="text-[10px] text-indigo-400 font-mono">Level 2</span>
                              <StatusBadge status={sub.isActive ? 'ACTIVE' : 'INACTIVE'} variant={sub.isActive ? 'emerald' : 'rose'} />
                              <button
                                onClick={() => {
                                  setEditingCategory(sub);
                                  setEditCatName(sub.name);
                                  setEditCatSlug(sub.slug);
                                  setEditCatParentId(sub.parentId || '');
                                  setEditCatIsActive(sub.isActive);
                                  setIsEditCategoryModalOpen(true);
                                }}
                                className="p-1 text-slate-400 hover:text-white rounded hover:bg-slate-800 transition"
                                title="Chỉnh sửa danh mục con"
                              >
                                <Edit3 className="h-3.5 w-3.5" />
                              </button>
                              <button
                                onClick={() => {
                                  setCategoryToDelete(sub);
                                  setIsDeleteCategoryModalOpen(true);
                                }}
                                className="p-1 text-rose-400 hover:text-rose-300 rounded hover:bg-rose-500/10 transition"
                                title="Xóa danh mục con"
                              >
                                <Trash2 className="h-3.5 w-3.5" />
                              </button>
                            </div>
                          </div>
                        );
                      })}
                      {subCats.length === 0 && (
                        <p className="text-xs text-slate-500 italic">No sub-categories assigned</p>
                      )}
                    </div>
                  </div>
                );
              })}
          </div>
        </div>
      )}

      {/* Tab 3: Pricing & Promotions */}
      {activeTab === 'pricing' && (
        <div className="rounded-xl border border-slate-800 bg-slate-950 p-6 space-y-6">
          <div className="flex items-center justify-between border-b border-slate-800 pb-4">
            <div>
              <h3 className="text-base font-bold text-white">Pricing Governance & Promotion Schedules</h3>
              <p className="text-xs text-slate-400">
                Invariant BR-013: Price modifications take effect on new orders only; historical placed orders maintain immutable price agreement.
              </p>
            </div>
            <span className="px-3 py-1 rounded bg-indigo-500/10 text-indigo-300 font-mono text-xs border border-indigo-500/20">
              BR-013 Protected
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="p-5 rounded-xl border border-slate-800 bg-slate-900/60 space-y-4">
              <h4 className="text-sm font-bold text-white flex items-center gap-2">
                <DollarSign className="h-4 w-4 text-emerald-400" />
                Active Effective Pricing Schedule
              </h4>
              <div className="space-y-2">
                {[
                  { sku: 'NIKE-PEG40-BLK-42', base: 2450000, promo: 2250000, promoName: 'Flash Autumn Sale', cur: 'VND' },
                  { sku: 'NIKE-PEG40-BLK-43', base: 2450000, promo: null, promoName: null, cur: 'VND' },
                  { sku: 'APP-AIRPODSPRO-WHT', base: 5990000, promo: null, promoName: null, cur: 'VND' },
                ].map((item) => (
                  <div key={item.sku} className="p-3 rounded-lg bg-slate-950 border border-slate-800/80 text-xs flex justify-between items-center">
                    <div>
                      <p className="font-mono font-bold text-white">{item.sku}</p>
                      {item.promoName && (
                        <span className="text-[10px] text-amber-400 font-semibold">{item.promoName}</span>
                      )}
                    </div>
                    <div className="text-right">
                      {item.promo ? (
                        <>
                          <p className="font-mono text-emerald-400 font-bold">
                            {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: item.cur }).format(item.promo)}
                          </p>
                          <p className="font-mono text-slate-500 line-through text-[11px]">
                            {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: item.cur }).format(item.base)}
                          </p>
                        </>
                      ) : (
                        <p className="font-mono text-slate-200 font-bold">
                          {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: item.cur }).format(item.base)}
                        </p>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="p-5 rounded-xl border border-slate-800 bg-slate-900/60 space-y-4">
              <h4 className="text-sm font-bold text-white flex items-center gap-2">
                <Calendar className="h-4 w-4 text-indigo-400" />
                Promotion Campaign History
              </h4>
              <div className="space-y-2">
                {[
                  { name: 'Flash Autumn Sale', discount: '-200,000 VND', valid: '2026-09-20 → 2026-10-05', status: 'ACTIVE' },
                  { name: 'Summer End Clearance', discount: '-15%', valid: '2026-08-01 → 2026-08-31', status: 'EXPIRED' },
                ].map((camp) => (
                  <div key={camp.name} className="p-3 rounded-lg bg-slate-950 border border-slate-800/80 text-xs flex justify-between items-center">
                    <div>
                      <p className="font-bold text-white">{camp.name}</p>
                      <p className="text-[11px] text-slate-400">{camp.valid}</p>
                    </div>
                    <div className="flex items-center gap-2">
                      <span className="font-mono text-emerald-400 font-bold">{camp.discount}</span>
                      <StatusBadge status={camp.status} />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Slide-over Drawer for Product Details & SKUs */}
      <SlideOverDrawer
        isOpen={!!selectedProduct}
        onClose={() => setSelectedProduct(null)}
        title={selectedProduct?.name || 'Product Details'}
        subtitle={`Category: ${selectedProduct?.categoryName} • Slug: ${selectedProduct?.slug}`}
        idToCopy={selectedProduct?.id}
        badge={selectedProduct ? <StatusBadge status={selectedProduct.status} /> : null}
      >
        {selectedProduct && (
          <div className="space-y-6">
            <div className="space-y-2">
              <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                Product Description
              </label>
              <p className="text-xs text-slate-300 bg-slate-950 p-4 rounded-xl border border-slate-800 leading-relaxed">
                {selectedProduct.description || 'No description provided.'}
              </p>
            </div>

            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                  Associated SKU Variants ({selectedProduct.skus?.length || 0})
                </label>
                <button
                  onClick={() => handleOpenAddSku(selectedProduct)}
                  className="flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-semibold bg-indigo-600/20 text-indigo-300 border border-indigo-500/30 hover:bg-indigo-600 hover:text-white transition"
                >
                  <Plus className="h-3 w-3" />
                  <span>Thêm SKU biến thể</span>
                </button>
              </div>

              <div className="border border-slate-800 rounded-xl overflow-hidden bg-slate-950 divide-y divide-slate-800">
                {(selectedProduct.skus || []).map((sku) => (
                  <div key={sku.id} className="p-4 space-y-2 text-xs">
                    <div className="flex items-center justify-between">
                      <span className="font-mono font-bold text-indigo-400">{sku.skuCode}</span>
                      <span className="font-mono text-emerald-400 font-bold">
                        {sku.basePrice
                          ? new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(sku.basePrice)
                          : 'Unpriced'}
                      </span>
                    </div>
                    <div className="flex flex-wrap gap-2 pt-1">
                      {Object.entries(sku.attributes || {}).map(([key, val]) => (
                        <span
                          key={key}
                          className="bg-slate-900 border border-slate-800 text-slate-300 px-2 py-0.5 rounded text-[11px]"
                        >
                          <strong className="text-slate-500 uppercase">{key}:</strong> {val}
                        </span>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {selectedProduct.status !== 'DISCONTINUED' && (
              <div className="pt-4 border-t border-slate-800 flex justify-end">
                <button
                  onClick={() => {
                    setProductToDiscontinue(selectedProduct);
                    setIsDiscontinueModalOpen(true);
                  }}
                  className="px-3.5 py-2 text-xs font-semibold text-rose-300 hover:text-white bg-rose-500/10 hover:bg-rose-600 border border-rose-500/30 rounded-lg transition flex items-center gap-2"
                >
                  <Ban className="h-3.5 w-3.5" />
                  <span>Ngừng kinh doanh sản phẩm</span>
                </button>
              </div>
            )}
          </div>
        )}
      </SlideOverDrawer>

      {/* Modal: Create Product */}
      <Modal
        isOpen={isProductModalOpen}
        onClose={() => setIsProductModalOpen(false)}
        title="Add New Catalog Product"
        subtitle="Registers base product entity under category hierarchy"
        footerActions={
          <>
            <button
              onClick={() => setIsProductModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-400 hover:text-white"
            >
              Cancel
            </button>
            <button
              disabled={!newProductName.trim() || createProductMutation.isPending}
              onClick={() => {
                createProductMutation.mutate({
                  name: newProductName,
                  slug: newProductSlug || newProductName.toLowerCase().replace(/[^a-z0-9]+/g, '-'),
                  description: newProductDesc,
                  categoryId: newProductCatId,
                });
              }}
              className="px-4 py-2 text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg transition"
            >
              {createProductMutation.isPending ? 'Creating...' : 'Create Product'}
            </button>
          </>
        }
      >
        <div className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 uppercase">Product Name</label>
            <input
              type="text"
              value={newProductName}
              onChange={(e) => {
                setNewProductName(e.target.value);
                if (!newProductSlug) {
                  setNewProductSlug(e.target.value.toLowerCase().replace(/[^a-z0-9]+/g, '-'));
                }
              }}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500"
              placeholder="e.g. Nike Invincible 3"
              required
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 uppercase">URL Slug</label>
            <input
              type="text"
              value={newProductSlug}
              onChange={(e) => setNewProductSlug(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-indigo-500"
              placeholder="e.g. nike-invincible-3"
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 uppercase">Category</label>
            <select
              value={newProductCatId}
              onChange={(e) => setNewProductCatId(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500"
            >
              {categories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} {c.parentId ? '(Sub-category)' : '(Root)'}
                </option>
              ))}
            </select>
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 uppercase">Description</label>
            <textarea
              rows={3}
              value={newProductDesc}
              onChange={(e) => setNewProductDesc(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500 placeholder-slate-500"
              placeholder="Detailed marketing and specification overview..."
            />
          </div>
        </div>
      </Modal>

      {/* Modal: Create Category */}
      <Modal
        isOpen={isCategoryModalOpen}
        onClose={() => setIsCategoryModalOpen(false)}
        title="Create Category (Max 2 Levels)"
        subtitle="Level 1 = Root; Level 2 = Sub-Category"
        footerActions={
          <>
            <button
              onClick={() => setIsCategoryModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-400 hover:text-white"
            >
              Cancel
            </button>
            <button
              disabled={!newCatName.trim() || createCategoryMutation.isPending}
              onClick={() => {
                createCategoryMutation.mutate({
                  name: newCatName,
                  slug: newCatSlug || newCatName.toLowerCase().replace(/[^a-z0-9]+/g, '-'),
                  parentId: newCatParentId || null,
                });
              }}
              className="px-4 py-2 text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg transition"
            >
              {createCategoryMutation.isPending ? 'Creating...' : 'Create Category'}
            </button>
          </>
        }
      >
        <div className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 uppercase">Category Name</label>
            <input
              type="text"
              value={newCatName}
              onChange={(e) => {
                setNewCatName(e.target.value);
                if (!newCatSlug) {
                  setNewCatSlug(e.target.value.toLowerCase().replace(/[^a-z0-9]+/g, '-'));
                }
              }}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500"
              placeholder="e.g. Trail Running"
              required
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 uppercase">Parent Category (Optional)</label>
            <select
              value={newCatParentId}
              onChange={(e) => setNewCatParentId(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500"
            >
              <option value="">None (Make this a Level 1 Root Category)</option>
              {categories
                .filter((c) => !c.parentId)
                .map((root) => (
                  <option key={root.id} value={root.id}>
                    {root.name} (Root)
                  </option>
                ))}
            </select>
            <p className="text-[11px] text-slate-500">
              * Hệ thống tuân thủ nghiêm ngặt mô hình phân cấp tối đa 2 tầng (SRS 6.2): Chỉ danh mục Gốc (Root) mới được chọn làm cha. Không cho phép tạo danh mục tầng 3.
            </p>
          </div>
        </div>
      </Modal>

      {/* Modal: Set Base Price */}
      <Modal
        isOpen={isPriceModalOpen}
        onClose={() => setIsPriceModalOpen(false)}
        title="Set Base Price (API-PRC-001)"
        subtitle="Modifies future catalog base price (BR-013)"
        footerActions={
          <>
            <button
              onClick={() => setIsPriceModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-400 hover:text-white"
            >
              Cancel
            </button>
            <button
              disabled={setPriceMutation.isPending}
              onClick={() => {
                setPriceMutation.mutate({
                  skuId: pricingSkuId,
                  amount: Number(newPriceAmount),
                  currency: 'VND',
                });
              }}
              className="px-4 py-2 text-xs font-semibold bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg transition"
            >
              {setPriceMutation.isPending ? 'Updating...' : 'Set Effective Price'}
            </button>
          </>
        }
      >
        <div className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 uppercase">SKU Identifier</label>
            <input
              type="text"
              value={pricingSkuId}
              onChange={(e) => setPricingSkuId(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-indigo-500"
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 uppercase">New Price (VND)</label>
            <input
              type="number"
              value={newPriceAmount}
              onChange={(e) => setNewPriceAmount(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-indigo-500"
            />
          </div>
        </div>
      </Modal>

      {/* Modal: Create Promotion (Task 04, FR-026, API-PRC-002) */}
      <Modal
        isOpen={isPromotionModalOpen}
        onClose={() => setIsPromotionModalOpen(false)}
        title="Create Promotional Discount"
        subtitle="Applies time-bounded promotion to selected SKU (ISO-8601 window)"
        footerActions={
          <>
            <button
              onClick={() => setIsPromotionModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-400 hover:text-white"
            >
              Cancel
            </button>
            {(() => {
              const isStartEndOrderValid =
                Boolean(promoStartAt && promoEndAt && new Date(promoStartAt).getTime() < new Date(promoEndAt).getTime());
              const isEndFutureValid = Boolean(promoEndAt && new Date(promoEndAt).getTime() > Date.now());
              const isFormValid =
                Boolean(promoName.trim() &&
                pricingSkuId.trim() &&
                Number(promoDiscountAmount) > 0 &&
                promoStartAt &&
                promoEndAt &&
                isStartEndOrderValid &&
                isEndFutureValid);

              return (
                <button
                  disabled={!isFormValid || createPromoMutation.isPending}
                  onClick={() => {
                    createPromoMutation.mutate({
                      skuId: pricingSkuId,
                      promotionName: promoName,
                      discountAmount: Number(promoDiscountAmount),
                      start_at: new Date(promoStartAt).toISOString(),
                      end_at: new Date(promoEndAt).toISOString(),
                      status: 'ACTIVE',
                    });
                  }}
                  className="px-4 py-2 text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed text-white rounded-lg transition"
                >
                  {createPromoMutation.isPending ? 'Scheduling...' : 'Schedule Promotion'}
                </button>
              );
            })()}
          </>
        }
      >
        <div className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 uppercase">Promotion Campaign Name</label>
            <input
              type="text"
              value={promoName}
              onChange={(e) => setPromoName(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500"
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 uppercase">SKU Target</label>
            <input
              type="text"
              value={pricingSkuId}
              onChange={(e) => setPricingSkuId(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-indigo-500"
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 uppercase">Discount Amount (VND)</label>
            <input
              type="number"
              value={promoDiscountAmount}
              onChange={(e) => setPromoDiscountAmount(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-indigo-500"
            />
          </div>

          {/* Time Frame Inputs (Task 04) */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-300 uppercase">Ngày bắt đầu hiệu lực (start_at)</label>
              <input
                type="datetime-local"
                value={promoStartAt}
                onChange={(e) => setPromoStartAt(e.target.value)}
                className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-indigo-500"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-300 uppercase">Ngày kết thúc hiệu lực (end_at)</label>
              <input
                type="datetime-local"
                value={promoEndAt}
                onChange={(e) => setPromoEndAt(e.target.value)}
                className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-indigo-500"
              />
            </div>
          </div>

          {promoStartAt && promoEndAt && new Date(promoStartAt).getTime() >= new Date(promoEndAt).getTime() && (
            <p className="text-xs text-rose-400 bg-rose-500/10 p-2.5 rounded-lg border border-rose-500/20">
              Cảnh báo: Ngày bắt đầu (start_at) phải nhỏ hơn ngày kết thúc (end_at).
            </p>
          )}

          {promoEndAt && new Date(promoEndAt).getTime() <= Date.now() && (
            <p className="text-xs text-rose-400 bg-rose-500/10 p-2.5 rounded-lg border border-rose-500/20">
              Cảnh báo: Ngày kết thúc (end_at) không được ở trong quá khứ.
            </p>
          )}
        </div>
      </Modal>

      {/* Modal: Edit Product & Product Media Upload (Task 07, API-CAT-004, API-CAT-008) */}
      <Modal
        isOpen={isEditProductModalOpen}
        onClose={() => setIsEditProductModalOpen(false)}
        title="Chỉnh sửa Thông tin Sản phẩm & Media (API-CAT-004/008)"
        subtitle={editingProduct ? `Cập nhật dữ liệu cho: ${editingProduct.name} (${editingProduct.id})` : ''}
        footerActions={
          <>
            <button
              onClick={() => setIsEditProductModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-400 hover:text-white"
            >
              Hủy bỏ
            </button>
            <button
              disabled={!editProductName.trim() || !editProductSlug.trim() || updateProductMutation.isPending}
              onClick={() => {
                if (editingProduct) {
                  updateProductMutation.mutate({
                    id: editingProduct.id,
                    name: editProductName.trim(),
                    slug: editProductSlug.trim(),
                    description: editProductDesc.trim(),
                    categoryId: editProductCatId,
                  });
                }
              }}
              className="px-4 py-2 text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white rounded-lg transition"
            >
              {updateProductMutation.isPending ? 'Đang lưu...' : 'Lưu thay đổi'}
            </button>
          </>
        }
      >
        <div className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 uppercase">Tên sản phẩm</label>
            <input
              type="text"
              value={editProductName}
              onChange={(e) => setEditProductName(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500"
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 uppercase">Slug đường dẫn</label>
            <input
              type="text"
              value={editProductSlug}
              onChange={(e) => setEditProductSlug(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-indigo-500"
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 uppercase">Mô tả sản phẩm</label>
            <textarea
              rows={3}
              value={editProductDesc}
              onChange={(e) => setEditProductDesc(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500"
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 uppercase">Danh mục trực thuộc</label>
            <select
              value={editProductCatId}
              onChange={(e) => setEditProductCatId(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500 font-mono"
            >
              {categories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} {c.parentId ? '(Sub-category)' : '(Root)'}
                </option>
              ))}
            </select>
          </div>

          {/* Media cấp Sản phẩm cha (sku_id = null per API-CAT-008) */}
          <div className="space-y-2 pt-2 border-t border-slate-800">
            <label className="text-xs font-semibold text-slate-300 uppercase flex items-center justify-between">
              <span>Hình ảnh đại diện cấp sản phẩm (sku_id = null)</span>
              <span className="text-[10px] text-indigo-400 font-mono">API-CAT-008</span>
            </label>
            <div className="flex gap-2">
              <input
                type="text"
                placeholder="Nhập URL hình ảnh sản phẩm (VD: https://...)"
                value={newProductMediaUrl}
                onChange={(e) => setNewProductMediaUrl(e.target.value)}
                className="flex-1 bg-slate-950 border border-slate-700 rounded-lg px-3 py-1.5 text-xs text-white font-mono focus:outline-none focus:border-indigo-500"
              />
              <button
                type="button"
                disabled={!newProductMediaUrl.trim() || uploadProductMediaMutation.isPending}
                onClick={() => {
                  if (editingProduct && newProductMediaUrl.trim()) {
                    uploadProductMediaMutation.mutate({
                      productId: editingProduct.id,
                      mediaUrl: newProductMediaUrl.trim(),
                    });
                    setProductMedia((prev) => [
                      ...prev,
                      {
                        id: `prod-media-${Date.now()}`,
                        fileName: 'product-image.jpg',
                        url: newProductMediaUrl.trim(),
                        isPrimary: prev.length === 0,
                      },
                    ]);
                  }
                }}
                className="px-3 py-1.5 text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white rounded-lg transition"
              >
                Thêm ảnh
              </button>
            </div>

            {productMedia.length > 0 && (
              <div className="grid grid-cols-3 gap-2 pt-1">
                {productMedia.map((m) => (
                  <div key={m.id} className="relative rounded-lg overflow-hidden border border-slate-800 p-1 bg-slate-950">
                    <img src={m.url} alt="Product Media" className="w-full h-16 object-cover rounded" />
                    <button
                      type="button"
                      onClick={() => setProductMedia((prev) => prev.filter((item) => item.id !== m.id))}
                      className="absolute top-1.5 right-1.5 p-0.5 rounded bg-black/70 text-rose-400 hover:text-white"
                      title="Xóa ảnh"
                    >
                      <Trash2 className="h-3 w-3" />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </Modal>

      {/* Modal: Edit Category (Task 08, API-CAT-005) */}
      <Modal
        isOpen={isEditCategoryModalOpen}
        onClose={() => setIsEditCategoryModalOpen(false)}
        title="Chỉnh sửa Danh mục (API-CAT-005)"
        subtitle="Tuân thủ tối đa 2 cấp phân cấp (Root Category → Sub-Category)"
        footerActions={
          <>
            <button
              onClick={() => setIsEditCategoryModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-400 hover:text-white"
            >
              Hủy bỏ
            </button>
            <button
              disabled={!editCatName.trim() || !editCatSlug.trim() || updateCategoryMutation.isPending}
              onClick={() => {
                if (editingCategory) {
                  updateCategoryMutation.mutate({
                    id: editingCategory.id,
                    name: editCatName.trim(),
                    slug: editCatSlug.trim(),
                    parentId: editCatParentId || null,
                    isActive: editCatIsActive,
                  });
                }
              }}
              className="px-4 py-2 text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white rounded-lg transition"
            >
              {updateCategoryMutation.isPending ? 'Đang cập nhật...' : 'Cập nhật danh mục'}
            </button>
          </>
        }
      >
        <div className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 uppercase">Tên danh mục</label>
            <input
              type="text"
              value={editCatName}
              onChange={(e) => setEditCatName(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500"
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 uppercase">Slug</label>
            <input
              type="text"
              value={editCatSlug}
              onChange={(e) => setEditCatSlug(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-indigo-500"
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 uppercase">Danh mục cha (Tối đa 2 cấp)</label>
            <select
              value={editCatParentId}
              onChange={(e) => setEditCatParentId(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500 font-mono"
            >
              <option value="">Không có (Danh mục gốc Root - Cấp 1)</option>
              {categories
                .filter((c) => !c.parentId && c.id !== editingCategory?.id)
                .map((root) => (
                  <option key={root.id} value={root.id}>
                    {root.name} (Root)
                  </option>
                ))}
            </select>
          </div>

          <div className="flex items-center justify-between p-3 rounded-lg bg-slate-950 border border-slate-800">
            <div>
              <p className="text-xs font-semibold text-white">Trạng thái hoạt động</p>
              <p className="text-[11px] text-slate-400">Cho phép người mua nhìn thấy danh mục trên Storefront</p>
            </div>
            <button
              type="button"
              onClick={() => setEditCatIsActive(!editCatIsActive)}
              className={`px-3 py-1 text-xs font-semibold rounded-lg transition ${
                editCatIsActive ? 'bg-emerald-600 text-white' : 'bg-rose-600 text-white'
              }`}
            >
              {editCatIsActive ? 'ACTIVE' : 'INACTIVE'}
            </button>
          </div>
        </div>
      </Modal>

      {/* Modal: Delete or Deactivate Category Guard (Task 08, BR-005) */}
      <Modal
        isOpen={isDeleteCategoryModalOpen}
        onClose={() => {
          setIsDeleteCategoryModalOpen(false);
          setCategoryToDelete(null);
        }}
        title="Xác nhận Xóa / Vô hiệu hóa Danh mục (BR-005)"
        subtitle={categoryToDelete ? `Danh mục: ${categoryToDelete.name} (${categoryToDelete.slug})` : ''}
        footerActions={
          <>
            <button
              onClick={() => {
                setIsDeleteCategoryModalOpen(false);
                setCategoryToDelete(null);
              }}
              className="px-4 py-2 text-xs font-semibold text-slate-400 hover:text-white"
            >
              Đóng
            </button>
            {categoryToDelete && getAssignedProductsCount(categoryToDelete.id) === 0 ? (
              <button
                disabled={deleteCategoryMutation.isPending}
                onClick={() => {
                  deleteCategoryMutation.mutate(categoryToDelete.id);
                }}
                className="px-4 py-2 text-xs font-semibold bg-rose-600 hover:bg-rose-500 disabled:opacity-50 text-white rounded-lg transition"
              >
                {deleteCategoryMutation.isPending ? 'Đang xóa...' : 'Xác nhận xóa danh mục'}
              </button>
            ) : null}
          </>
        }
      >
        <div className="space-y-4">
          {categoryToDelete && getAssignedProductsCount(categoryToDelete.id) > 0 ? (
            <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 space-y-2 text-xs">
              <div className="flex items-center gap-2 text-rose-400 font-bold">
                <AlertTriangle className="h-4 w-4" />
                <span>Vi phạm ràng buộc toàn vẹn BR-005</span>
              </div>
              <p className="leading-relaxed">
                Không thể xóa hoặc vô hiệu hóa danh mục <strong className="text-white font-mono">{categoryToDelete.name}</strong> vì đang có{' '}
                <strong className="text-rose-400 font-mono">{getAssignedProductsCount(categoryToDelete.id)} sản phẩm</strong> trực thuộc.
              </p>
              <p className="text-[11px] text-slate-400">
                Quy tắc nghiệp vụ BR-005 cấm xóa danh mục khi còn sản phẩm tham chiếu. Vui lòng chuyển các sản phẩm sang danh mục khác trước khi thực hiện.
              </p>
            </div>
          ) : (
            <div className="p-4 rounded-xl bg-amber-500/10 border border-amber-500/30 text-amber-300 space-y-2 text-xs">
              <div className="flex items-center gap-2 text-amber-400 font-bold">
                <AlertTriangle className="h-4 w-4" />
                <span>Cảnh báo thao tác xóa danh mục</span>
              </div>
              <p className="leading-relaxed">
                Danh mục này hiện không có sản phẩm nào trực thuộc (0 sản phẩm). Bạn có chắc chắn muốn xóa vĩnh viễn danh mục này không?
              </p>
            </div>
          )}
        </div>
      </Modal>

      {/* Modal: Discontinue Product Confirmation */}
      <Modal
        isOpen={isDiscontinueModalOpen}
        onClose={() => {
          setIsDiscontinueModalOpen(false);
          setProductToDiscontinue(null);
        }}
        title="Xác nhận ngừng kinh doanh sản phẩm"
        subtitle={`Sản phẩm: ${productToDiscontinue?.name || ''} (${productToDiscontinue?.id || ''})`}
        footerActions={
          <>
            <button
              onClick={() => {
                setIsDiscontinueModalOpen(false);
                setProductToDiscontinue(null);
              }}
              className="px-4 py-2 text-xs font-semibold text-slate-400 hover:text-white"
            >
              Hủy bỏ
            </button>
            <button
              disabled={discontinueProductMutation.isPending}
              onClick={() => {
                if (productToDiscontinue) {
                  discontinueProductMutation.mutate(productToDiscontinue.id);
                }
              }}
              className="px-4 py-2 text-xs font-semibold bg-rose-600 hover:bg-rose-500 text-white rounded-lg transition flex items-center gap-1.5"
            >
              <Ban className="h-3.5 w-3.5" />
              <span>{discontinueProductMutation.isPending ? 'Đang xử lý...' : 'Xác nhận ngừng kinh doanh'}</span>
            </button>
          </>
        }
      >
        <div className="space-y-4">
          <div className="p-3.5 rounded-lg bg-amber-500/10 border border-amber-500/30 text-amber-300 text-xs flex items-start gap-2.5">
            <AlertTriangle className="h-5 w-5 shrink-0 text-amber-400 mt-0.5" />
            <div className="space-y-1">
              <p className="font-semibold text-white">Cảnh báo vòng đời danh mục (Catalog Governance)</p>
              <p>Sản phẩm sẽ bị ẩn khỏi storefront khách hàng nhưng vẫn lưu vết cho các đơn hàng cũ và lịch sử tồn kho.</p>
            </div>
          </div>
          <p className="text-xs text-slate-400">
            Sau khi chuyển sang trạng thái <strong>DISCONTINUED</strong>, sản phẩm không thể tiếp tục nhận đặt hàng từ phía khách hàng. Hệ thống nghiêm cấm thao tác xóa cứng (Hard Delete) để bảo toàn tính toàn vẹn dữ liệu cho các phân hệ Order và Inventory.
          </p>
        </div>
      </Modal>

      {/* Modal: Master Attributes Management (STT 06) */}
      <Modal
        isOpen={isMasterAttrModalOpen}
        onClose={() => setIsMasterAttrModalOpen(false)}
        title="Quản lý Thuộc tính Chuẩn hóa (Master Attributes)"
        subtitle="Danh mục các thuộc tính dùng chung cho biến thể SKU (Color, Size, Material, Storage)"
        footerActions={
          <button
            onClick={() => setIsMasterAttrModalOpen(false)}
            className="px-4 py-2 text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-white rounded-lg transition"
          >
            Đóng
          </button>
        }
      >
        <div className="space-y-6">
          {/* Create New Master Attribute */}
          <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 space-y-3">
            <h4 className="text-xs font-bold text-white uppercase tracking-wider flex items-center gap-1.5">
              <Plus className="h-3.5 w-3.5 text-indigo-400" />
              <span>Thêm thuộc tính mới</span>
            </h4>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div className="space-y-1">
                <label className="text-[11px] font-semibold text-slate-300">Mã thuộc tính (Code)</label>
                <input
                  type="text"
                  placeholder="VD: COLOR, SIZE, RAM"
                  value={newAttrCode}
                  onChange={(e) => setNewAttrCode(e.target.value.toUpperCase())}
                  className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-indigo-500"
                />
              </div>
              <div className="space-y-1">
                <label className="text-[11px] font-semibold text-slate-300">Tên hiển thị</label>
                <input
                  type="text"
                  placeholder="VD: Màu sắc, Kích cỡ, Bộ nhớ RAM"
                  value={newAttrName}
                  onChange={(e) => setNewAttrName(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500"
                />
              </div>
            </div>
            <div className="space-y-1">
              <label className="text-[11px] font-semibold text-slate-300">Các giá trị mặc định (phân cách bởi dấu phẩy)</label>
              <input
                type="text"
                placeholder="VD: Đen, Trắng, Xanh Navy, Đỏ"
                value={newAttrValues}
                onChange={(e) => setNewAttrValues(e.target.value)}
                className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500"
              />
            </div>
            <div className="flex justify-end">
              <button
                type="button"
                onClick={handleAddMasterAttribute}
                className="px-3.5 py-1.5 text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg transition"
              >
                Thêm thuộc tính
              </button>
            </div>
          </div>

          {/* List Existing Attributes */}
          <div className="space-y-2">
            <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
              Danh sách thuộc tính hiện có ({masterAttributes.length})
            </label>
            <div className="space-y-2.5 max-h-60 overflow-y-auto">
              {masterAttributes.map((attr) => (
                <div key={attr.id} className="p-3 rounded-lg bg-slate-950 border border-slate-800 space-y-2">
                  <div className="flex items-center justify-between">
                    <div>
                      <span className="font-bold text-white text-xs">{attr.name}</span>
                      <span className="font-mono text-indigo-400 text-[11px] ml-2 font-semibold">({attr.code})</span>
                    </div>
                    <span className="text-[10px] text-slate-500 font-mono">{attr.values.length} giá trị</span>
                  </div>
                  <div className="flex flex-wrap gap-1.5">
                    {attr.values.map((val) => (
                      <span
                        key={val}
                        className="px-2 py-0.5 rounded text-[10px] font-mono bg-slate-900 text-slate-300 border border-slate-800"
                      >
                        {val}
                      </span>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </Modal>

      {/* Modal: Add SKU Variant with Media Dropzone & Auto-uppercase (STT 06 & STT 13) */}
      <Modal
        isOpen={isSkuModalOpen}
        onClose={() => setIsSkuModalOpen(false)}
        title="Thêm Biến thể SKU Mới (Variant Creation)"
        subtitle={targetProduct ? `Sản phẩm gốc: ${targetProduct.name} (#${targetProduct.id})` : ''}
        footerActions={
          <>
            <button
              onClick={() => setIsSkuModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-400 hover:text-white"
            >
              Hủy bỏ
            </button>
            <button
              disabled={!isSkuCodeValid}
              onClick={handleSaveSku}
              className="px-4 py-2 text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white rounded-lg transition shadow-sm"
            >
              Lưu biến thể SKU
            </button>
          </>
        }
      >
        <div className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {/* SKU Code Input (STT 13 Auto-uppercase & regex ^[A-Z0-9_-]{4,32}$) */}
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <label className="text-xs font-semibold text-slate-300 uppercase">
                  Mã SKU (SKU Code) *
                </label>
                <span
                  className={`text-[10px] font-mono font-semibold ${
                    isSkuCodeValid ? 'text-emerald-400' : 'text-slate-500'
                  }`}
                >
                  {isSkuCodeValid ? 'Hợp lệ' : '^[A-Z0-9_-]{4,32}$'}
                </span>
              </div>
              <input
                type="text"
                placeholder="VD: NIKE-PEG40-NVY-42"
                value={skuCode}
                onChange={(e) => setSkuCode(e.target.value.toUpperCase().replace(/[^A-Z0-9_-]/g, ''))}
                className={`w-full bg-slate-950 border rounded-lg px-3 py-2 text-xs text-white font-mono focus:outline-none ${
                  skuCode && !isSkuCodeValid
                    ? 'border-rose-500 focus:border-rose-500'
                    : 'border-slate-700 focus:border-indigo-500'
                }`}
              />
              <p className="text-[10px] text-slate-500">
                Tự động viết hoa & loại bỏ ký tự đặc biệt. Yêu cầu: 4 - 32 ký tự chữ hoa, số, gạch nối hoặc gạch dưới.
              </p>
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-300 uppercase">Mã vạch (Barcode / UPC / EAN)</label>
              <input
                type="text"
                placeholder="VD: 883419001199"
                value={skuBarcode}
                onChange={(e) => setSkuBarcode(e.target.value)}
                className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-indigo-500"
              />
            </div>
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 uppercase">Giá niêm yết cơ sở (Base Price VND)</label>
            <input
              type="number"
              value={skuPrice}
              onChange={(e) => setSkuPrice(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-indigo-500"
            />
          </div>

          {/* Master Attributes Mapping */}
          <div className="space-y-2">
            <label className="text-xs font-semibold text-slate-300 uppercase">
              Thuộc tính biến thể (Theo Master Attributes)
            </label>
            <div className="grid grid-cols-2 gap-3 p-3 rounded-xl bg-slate-950 border border-slate-800">
              {masterAttributes.map((attr) => (
                <div key={attr.id} className="space-y-1">
                  <label className="text-[11px] font-medium text-slate-400">{attr.name}</label>
                  <select
                    value={skuAttributes[attr.code.toLowerCase()] || ''}
                    onChange={(e) =>
                      setSkuAttributes((prev) => ({
                        ...prev,
                        [attr.code.toLowerCase()]: e.target.value,
                      }))
                    }
                    className="w-full bg-slate-900 border border-slate-700 rounded-lg px-2.5 py-1.5 text-xs text-white focus:outline-none focus:border-indigo-500"
                  >
                    <option value="">-- Không chọn --</option>
                    {attr.values.map((val) => (
                      <option key={val} value={val}>
                        {val}
                      </option>
                    ))}
                  </select>
                </div>
              ))}
            </div>
          </div>

          {/* Media Dropzone & Management (STT 06) */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <label className="text-xs font-semibold text-slate-300 uppercase flex items-center gap-1.5">
                <ImageIcon className="h-3.5 w-3.5 text-indigo-400" />
                <span>Thư viện hình ảnh SKU (Media Gallery)</span>
              </label>
              <span className="text-[10px] text-slate-500 font-mono">{skuMedia.length} hình ảnh</span>
            </div>

            <label className="border-2 border-dashed border-slate-700 hover:border-indigo-500/60 rounded-xl p-4 flex flex-col items-center justify-center gap-2 cursor-pointer transition bg-slate-950/60 hover:bg-slate-900/40">
              <UploadCloud className="h-6 w-6 text-indigo-400" />
              <div className="text-center">
                <p className="text-xs font-semibold text-white">Nhấp để chọn hoặc kéo thả ảnh vào đây</p>
                <p className="text-[10px] text-slate-500">Hỗ trợ PNG, JPG, WEBP. Ảnh đầu tiên tự động thành ảnh đại diện.</p>
              </div>
              <input
                type="file"
                multiple
                accept="image/*"
                onChange={handleUploadMedia}
                className="hidden"
              />
            </label>

            {skuMedia.length > 0 && (
              <div className="grid grid-cols-3 sm:grid-cols-4 gap-2.5 pt-2">
                {skuMedia.map((media) => (
                  <div
                    key={media.id}
                    className={`relative group rounded-lg overflow-hidden border p-1 bg-slate-950 flex flex-col justify-between ${
                      media.isPrimary ? 'border-amber-500/60 ring-1 ring-amber-500/30' : 'border-slate-800'
                    }`}
                  >
                    <img
                      src={media.url}
                      alt="SKU Preview"
                      className="w-full h-20 object-cover rounded"
                    />
                    <div className="mt-1 flex items-center justify-between text-[10px]">
                      {media.isPrimary ? (
                        <span className="px-1.5 py-0.5 rounded text-[9px] font-bold bg-amber-500/20 text-amber-300 border border-amber-500/30 flex items-center gap-0.5">
                          <Star className="h-2.5 w-2.5 fill-amber-400" /> Chính
                        </span>
                      ) : (
                        <button
                          type="button"
                          onClick={() => handleSetPrimaryMedia(media.id)}
                          className="text-slate-400 hover:text-amber-300 transition text-[10px]"
                        >
                          Đặt chính
                        </button>
                      )}
                      <button
                        type="button"
                        onClick={() => handleRemoveMedia(media.id)}
                        className="text-rose-400 hover:text-rose-300 p-0.5"
                        title="Xóa ảnh"
                      >
                        <Trash2 className="h-3 w-3" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </Modal>
    </div>
  );
};
