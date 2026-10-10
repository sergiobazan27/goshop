import { Component } from '@angular/core';
import { LucideArrowRight, LucideClock } from '@lucide/angular';
import { RecentOrder } from '../recent-order/recent-order';

@Component({
  selector: 'app-recent-orders',
  imports: [RecentOrder, LucideArrowRight, LucideClock],
  templateUrl: './recent-orders.html',
  host: { class: 'block min-w-0' },
})
export class RecentOrders {}
