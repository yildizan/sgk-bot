# sgk-bot

### About

Sgk Bot is a Discord bot built to measure online duration of users in a channel.

When an user commands to start watch with `!sgk ölç`, bot triggers an event to work minutely
and update online users' duration. The bot also salutes users when their statuses turn to online
for first time as a bonus 😄

You can invite the bot to your server with this [link](https://discordapp.com/oauth2/authorize?client_id=675002156948127794&permissions=10240&scope=bot).

### Commands
* `!sgk help`: displays commands and parameters
* `!sgk ölç`: watches channel members' online durations
* `!sgk sal`: stops watch
* `!sgk durum`: displays durations
* `!sgk durum [ilk|son|ben]`: display duration of [first | last | self] user
* `!sgk clear`: deletes messages of bot and commands

### License
This repository is under the [MIT license](https://github.com/yildizan/sgk-bot/blob/master/LICENSE.md).