# Stock tracking and reminders

## How stock tracking works

Set a medicine's **Remaining stock** in its stock settings. When you mark a dose as taken, MedTimer subtracts the dose
amount from the stock; skipped doses do not use stock. Stock cannot fall below zero. You can change the remaining stock
or use **Refill now** to add the configured refill size. A stock reminder can also be enabled without entering an
initial stock amount.

Stock is stored as a number; its display unit is inferred from the medicine's active dose reminders. Some singular and
plural forms, such as `tablet` and `tablets`, are recognized as the same unit. Other mixed or missing units can make the
stock unit unclear. Keep units consistent between doses; medTimer does not convert units. A dose without a readable
number cannot be deducted from stock. If the units are inconsistent, the stock is still deducted.

For dose amounts, MedTimer reads the first number using your phone's number format. For example, `1 capsule (25 mg)`
uses `1` as the amount and `capsule` as the unit; `25 mg` is not included. Use your phone's decimal separator when
entering amounts. MedTimer will show the resulting value and the detected unit in the dose's amount editor.

## Stock reminders

Add an **Out of stock reminder** to a medicine's reminders and set the stock threshold and frequency. The reminder is
eligible when the remaining stock reaches or falls below its threshold. Choose whether to notify once when stock first
reaches the threshold, after every dose while below it, or once per day. Disable the reminder to stop these alerts.

Stock reminders are separate from dose reminders: they notify you about low stock and do not themselves deduct stock.
