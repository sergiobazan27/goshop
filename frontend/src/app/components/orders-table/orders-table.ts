import { Component } from '@angular/core';
import { LucideChevronRight } from '@lucide/angular';

@Component({
  selector: 'app-orders-table',
  imports: [LucideChevronRight],
  templateUrl: './orders-table.html',
  host: { class: 'block min-w-0' },
})
export class OrdersTable {}
