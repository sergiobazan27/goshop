export interface ProductRequest {
  name: string;
  description: string;
  price: number;
  available: boolean;
}

export interface Product extends ProductRequest {
  id: string;
}
