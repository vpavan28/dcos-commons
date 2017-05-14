package main

import (
	"github.com/mesosphere/dcos-commons/cli"
	"gopkg.in/alecthomas/kingpin.v2"
)

func main() {
	app := cli.New()

	cli.HandleConfigSection(app)
	cli.HandleEndpointsSection(app)
	cli.HandlePlanSection(app)
	cli.HandlePodsSection(app)
	cli.HandleStateSection(app)

	handlePxSection(app)

	kingpin.MustParse(app.Parse(cli.GetArguments()))
}

func runNodeList(c *kingpin.ParseContext) error {
	cli.PrintJSON(cli.HTTPGet("v1/px/status"))
	return nil
}

func runVolumeList(c *kingpin.ParseContext) error {
	cli.PrintJSON(cli.HTTPGet("v1/px/volumes"))
	return nil
}

func handlePxSection(app *kingpin.Application) {

	app.Command("status", "List the status of the nodes in the PX cluster").
		Action(runNodeList)

	volume := app.Command("volume", "Manage volumes")
	volume.Command("list", "List the volumes").
		Action(runVolumeList)
}
