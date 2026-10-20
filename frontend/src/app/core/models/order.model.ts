import { DiningTable } from './dining-table.model';

export interface OrderItem {
  productId: string;
  name: string;
  unitPrice: number;
  quantity: number;
  total: number;
}

export interface DiningOrder {
  id: string;
  table: DiningTable;
  status: 'OPEN' | 'CLOSED';
  revision: number;
  createdAt: string;
  openedBy: string;
  closedAt: string | null;
  closedBy: string | null;
  items: OrderItem[];
  total: number;
}

export interface TableOrder {
  table: DiningTable;
  orderId: string | null;
  total: number;
  quantity: number;
}

export interface OrderItemRequest {
  quantity: number;
  revision: number;
}

export interface CloseOrderRequest {
  revision: number;
}

export interface OrderHistoryEntry {
  id: string;
  tableName: string;
  closedAt: string;
  closedBy: string;
  total: number;
}
