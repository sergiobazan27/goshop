import { DiningOrder, TableOrder } from './order.model';

export interface OverviewMetrics {
  totalTables: number;
  occupiedTables: number;
  openOrders: number;
  availableProducts: number;
}

export interface OverviewOrder {
  id: string;
  tableName: string;
  status: DiningOrder['status'];
  createdAt: string;
  updatedAt: string;
  quantity: number;
  products: string;
  total: number;
}

export interface Overview {
  generatedAt: string;
  metrics: OverviewMetrics;
  tables: TableOrder[];
  recentOrders: OverviewOrder[];
  activity: OverviewOrder[];
}
