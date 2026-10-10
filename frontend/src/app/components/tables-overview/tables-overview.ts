import { Component } from '@angular/core';
import { LucideArrowRight } from '@lucide/angular';
import { TableCard } from '../table-card/table-card';

@Component({
  selector: 'app-tables-overview',
  imports: [TableCard, LucideArrowRight],
  templateUrl: './tables-overview.html',
  host: { class: 'block min-w-0' },
})
export class TablesOverview {}
