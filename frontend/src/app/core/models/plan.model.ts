export interface PlanRequest {
  name: string;
  maxTables: number;
  maxUsers: number;
  maxProducts: number;
  active: boolean;
}

export interface Plan extends PlanRequest {
  id: string;
  createdAt: string;
  updatedAt: string;
}

export interface PlanListQuery {
  search: string;
  active: '' | 'true' | 'false';
  page: number;
  size: number;
}
