import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideArrowRight } from '@lucide/angular';
import { TableCard } from '../table-card/table-card';
import { TableOrder } from '../../core/models/order.model';

@Component({
  selector: 'app-tables-overview',
  imports: [TableCard, RouterLink, LucideArrowRight],
  templateUrl: './tables-overview.html',
  host: { class: 'block min-w-0' },
})
export class TablesOverview {
  readonly tables = input.required<TableOrder[]>();
  readonly total = input.required<number>();
}
