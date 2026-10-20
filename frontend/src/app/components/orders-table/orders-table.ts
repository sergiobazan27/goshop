import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideChevronRight } from '@lucide/angular';
import { OverviewOrder } from '../../core/models/overview.model';

@Component({
  selector: 'app-orders-table',
  imports: [DatePipe, DecimalPipe, RouterLink, LucideChevronRight],
  templateUrl: './orders-table.html',
  host: { class: 'block min-w-0' },
})
export class OrdersTable {
  readonly orders = input.required<OverviewOrder[]>();
}
