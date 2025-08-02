# IPGuard

A simple PaperMC plugin that blocks players from countries you don't allow, using geolocation from IP2Location LITE.

## Features

- Block or allow players by country code (ISO alpha-2)
- Exclude specific IPs from blocking
- Uses IP2Location LITE geolocation database
- Simple configuration via `config.yml`

## Installation

1. Download the latest release `.jar` file.
2. Drop it into your `plugins/` folder on your PaperMC server.
3. Start the server to generate the config.
4. Edit `plugins/IPguard/config.yml` to define allowed countries and IP exceptions.

## Configuration Example

```yaml
allowed-countries:
  - SK
  - CZ
  - DE
//This means poeple from Slovakia,Czech Republic and Germany can join the server

excluded-ips:
  - 192.0.2.0
  - 192.0.2.255
//IPs that are outside of the allowed countries that can join too



