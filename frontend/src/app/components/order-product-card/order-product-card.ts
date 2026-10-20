import { DecimalPipe } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { LucidePlus } from '@lucide/angular';
import { Product } from '../../core/models/product.model';

@Component({
  selector: 'app-order-product-card',
  imports: [DecimalPipe, LucidePlus],
  templateUrl: './order-product-card.html',
  styleUrl: './order-product-card.css',
})
export class OrderProductCard {
  readonly product = input.required<Product>();
  readonly quantity = input(0);
  readonly disabled = input(false);
  readonly added = output<void>();
}
