import { Component, input } from '@angular/core';

@Component({
  selector: 'app-table-card',
  templateUrl: './table-card.html',
  host: { class: 'block min-w-0' },
})
export class TableCard {
  readonly number = input.required<string>();
  readonly details = input.required<string>();
  readonly occupied = input(false);
}
