import { Component, input } from '@angular/core';

@Component({
  selector: 'app-recent-order',
  templateUrl: './recent-order.html',
  host: { class: 'block' },
})
export class RecentOrder {
  readonly table = input.required<string>();
  readonly order = input.required<string>();
  readonly quantity = input.required<string>();
  readonly time = input.required<string>();
}
