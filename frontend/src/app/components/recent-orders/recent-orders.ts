import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideArrowRight, LucideClock } from '@lucide/angular';
import { RecentOrder } from '../recent-order/recent-order';
import { OverviewOrder } from '../../core/models/overview.model';

@Component({
  selector: 'app-recent-orders',
  imports: [RecentOrder, RouterLink, LucideArrowRight, LucideClock],
  templateUrl: './recent-orders.html',
  host: { class: 'block min-w-0' },
})
export class RecentOrders {
  readonly orders = input.required<OverviewOrder[]>();
}
