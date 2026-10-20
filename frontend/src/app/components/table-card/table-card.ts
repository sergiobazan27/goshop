import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TableOrder } from '../../core/models/order.model';

@Component({
  selector: 'app-table-card',
  imports: [RouterLink],
  templateUrl: './table-card.html',
  host: { class: 'block min-w-0' },
})
export class TableCard {
  readonly entry = input.required<TableOrder>();
}
